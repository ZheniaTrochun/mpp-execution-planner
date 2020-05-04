package com.yevhenii.cluster.planner.server.graphs

import cats.Show
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model.{GraphParameters, Node, OrientedEdge, OrientedGraph}
import com.yevhenii.cluster.planner.server.graphs.GraphShow._
import com.yevhenii.cluster.planner.server.utils.{Continuation, Finish, Trampoline}

import scala.util.Random

object GraphRandomizer extends LazyLogging {

  val rand = new Random()
  val MaxNumberOfTries = 20
  val MaxNumberOfRetriesOfFittingEdges = 1000
  val Epsilon = 0.0001

  def createRandomOrientedGraph(parameters: GraphParameters): OrientedGraph = {
    val nodes = createRandomNodes(parameters)
    val edges = createRandomEdges(nodes, parameters)

    fitEdges(nodes, edges, parameters)
  }

  private def createRandomNodes(parameters: GraphParameters): List[Node] = {
    (1 to parameters.numberOfNodes)
      .map(i => (i, randomIntBetweenInclusive(parameters.minimalNodeWeight, parameters.maximumNodeWeight)))
      .map { case (index, weight) => Node(s"$index", weight) }
      .toList
  }

  private def createRandomEdges(nodes: List[Node], parameters: GraphParameters): List[OrientedEdge] = {
    val numberOfEdges = randomIntBetweenInclusive((nodes.size * 0.7).toInt, (nodes.size * 1.7).toInt)

    (1 to numberOfEdges)
      .map(_ => randomIntBetweenInclusive(parameters.minimalEdgeWeight, parameters.maximumEdgeWeight))
      .foldLeft(List.empty[OrientedEdge]) { (edges, weight) =>
        tryToCreateEdge(nodes, edges, weight)
          .map(edge => edge :: edges)
          .getOrElse(edges)
      }
  }

  private def tryToCreateEdge(nodes: List[Node], edges: List[OrientedEdge], weight: Int): Option[OrientedEdge] = {
    Stream.continually {
      val source = rand.nextInt(nodes.size) + 1
      val target = rand.nextInt(nodes.size) + 1

      createOrientedEdge(source, target, weight)
    }
      .take(MaxNumberOfTries)
      .find(isEdgeSuits(nodes, edges)(_))
  }

  private def isEdgeSuits(nodes: List[Node], edges: List[OrientedEdge])(edge: OrientedEdge): Boolean = {
    !edges.exists(oldEdge => (edge.target == oldEdge.target) && (edge.source == oldEdge.source)) &&
      GraphOps.checkGraphForCycles(OrientedGraph(edge :: edges ::: nodes))
  }

  private def createOrientedEdge(source: Int, target: Int, weight: Int): OrientedEdge = {
    OrientedEdge(s"$source-$target", weight, source.toString, target.toString)
  }

  private def fitEdges(nodes: List[Node], edges: List[OrientedEdge], parameters: GraphParameters): OrientedGraph = {

    val resultTrampoline = fitEdgesSafe(nodes, edges, parameters, 0)
    Trampoline.calculate(resultTrampoline)
  }

  private def fitEdgesSafe(nodes: List[Node], edges: List[OrientedEdge], parameters: GraphParameters, retry: Int): Trampoline[OrientedGraph] = {
    val targetCorrelation = parameters.correlation

    val nodeWeights = nodes.map(_.weight).sum
    val edgeWeights = edges.map(_.weight).sum

    val correlation = GraphOps.correlationOfConnections(nodeWeights, edgeWeights)

    if (retry > MaxNumberOfRetriesOfFittingEdges) {
      val graph = OrientedGraph(nodes ::: edges)
      logger.warn(s"Could not fit edges fine, exhausted number of retries, target correlation = $targetCorrelation, current correlation = $correlation")
      logger.info(s"\n${Show[OrientedGraph].show(graph)}\n")
      Finish(graph)

    } else {

      if (math.abs(targetCorrelation - correlation) <= Epsilon) {
        Finish(OrientedGraph(nodes ::: edges))
      } else {

        val shuffledEdges = Random.shuffle(edges)

        if (correlation < targetCorrelation) {
          decreaseEdgeWeights(nodes, shuffledEdges, parameters, retry, nodeWeights, edgeWeights, correlation)
        } else {
          increaseEdgeWeights(nodes, shuffledEdges, parameters, retry, nodeWeights, edgeWeights, correlation)
        }
      }
    }
  }

  private def increaseEdgeWeights(
    nodes: List[Node],
    shuffledEdges: List[OrientedEdge],
    parameters: GraphParameters,
    retry: Int,
    nodeWeights: Int,
    edgeWeights: Int,
    correlation: Double
  ): Trampoline[OrientedGraph] = {

    val targetCorrelation = parameters.correlation

    val correlationWithNewEdge = GraphOps.correlationOfConnections(nodeWeights, edgeWeights + parameters.minimalEdgeWeight)

    if (math.abs(targetCorrelation - correlationWithNewEdge) < math.abs(targetCorrelation - correlation)) {

      val maxNumberOfEdges = (0 until parameters.numberOfNodes).sum

      val newEdgeOpt = if (shuffledEdges.size < maxNumberOfEdges) {
        tryToCreateEdge(nodes, shuffledEdges, randomIntBetweenInclusive(parameters.minimalEdgeWeight, parameters.maximumEdgeWeight))
      } else {
        None
      }

      newEdgeOpt match {
        case Some(edge) =>
          Continuation(() => fitEdgesSafe(nodes, edge :: shuffledEdges, parameters, retry + 1))
        case None =>
          increaseEdgesWithoutAddingNewEdge(nodes, shuffledEdges, parameters, retry)
      }
    } else {

      incrementOneEdge(nodes, shuffledEdges, parameters, retry)
    }
  }

  private def incrementOneEdge(nodes: List[Node], shuffledEdges: List[OrientedEdge], parameters: GraphParameters, retry: Int): Trampoline[OrientedGraph] = {
    val shouldBeIncremented = shuffledEdges.find(edge => edge.weight < parameters.maximumEdgeWeight)
    shouldBeIncremented match {
      case None =>
        val graph = OrientedGraph(nodes ::: shuffledEdges)
        logger.warn(s"Cannot increment edge for graph:\n${Show[OrientedGraph].show(graph)}\n")
        Finish(graph)

      case Some(edge) =>
        val updatedEdges = edge.copy(weight = edge.weight + 1) :: shuffledEdges.filterNot(_ == edge)
        Continuation(() => fitEdgesSafe(nodes, updatedEdges, parameters, retry + 1))
    }
  }

  private def increaseEdgesWithoutAddingNewEdge(
    nodes: List[Node],
    shuffledEdges: List[OrientedEdge],
    parameters: GraphParameters,
    retry: Int
  ): Trampoline[OrientedGraph] = {

    val graph = OrientedGraph(nodes ::: shuffledEdges)

    shuffledEdges.find(_.weight < parameters.maximumEdgeWeight) match {
      case Some(_) =>
        val updatedEdges = shuffledEdges.map(edge => edge.copy(weight = math.min(edge.weight + 1, parameters.maximumEdgeWeight)))
        Continuation(() => fitEdgesSafe(nodes, updatedEdges, parameters, retry + 1))
      case None =>
        decreaseNodeWeight(nodes, shuffledEdges, parameters, graph, retry)
    }
  }

  private def decreaseNodeWeight(
    nodes: List[Node],
    shuffledEdges: List[OrientedEdge],
    parameters: GraphParameters,
    graph: OrientedGraph,
    retry: Int
  ): Trampoline[OrientedGraph] = {
    val shuffledNodes = rand.shuffle(nodes)
    val firstSuitableNode = shuffledNodes.find(_.weight > parameters.minimalNodeWeight)

    firstSuitableNode match {
      case Some(node) =>
        val restOfNodes = nodes.filterNot(_ == node)
        Continuation(() => fitEdgesSafe(node.copy(weight = node.weight - 1) :: restOfNodes, shuffledEdges, parameters, retry + 1))
      case None =>
        Finish(graph)
    }
  }

  private def decreaseEdgeWeights(
    nodes: List[Node],
    shuffledEdges: List[OrientedEdge],
    parameters: GraphParameters,
    retry: Int,
    nodeWeights: Int,
    edgeWeights: Int,
    correlation: Double
  ): Trampoline[OrientedGraph] = {

    val targetCorrelation = parameters.correlation

    val correlationWithoutMaxEdge = GraphOps.correlationOfConnections(nodeWeights, edgeWeights - parameters.maximumEdgeWeight)

    if (math.abs(targetCorrelation - correlationWithoutMaxEdge) < math.abs(targetCorrelation - correlation)) {

      Continuation(() => fitEdgesSafe(nodes, shuffledEdges.tail, parameters, retry + 1))
    } else {

      val reducedEdgeWeight = shuffledEdges.head.weight - 1

      val updatedEdges =
        if (reducedEdgeWeight < parameters.minimalEdgeWeight) shuffledEdges.tail
        else shuffledEdges.head.copy(weight = reducedEdgeWeight) :: shuffledEdges.tail

      Continuation(() => fitEdgesSafe(nodes, updatedEdges, parameters, retry + 1))
    }
  }

  private def randomIntBetweenInclusive(min: Int, max: Int): Int = {
    rand.nextInt(max - min + 1) + min
  }
}
