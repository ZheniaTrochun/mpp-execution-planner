package com.yevhenii.cluster.planner.server.graphs

import cats.Show
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model.{GraphParameters, Node, OrientedEdge, OrientedGraph}
import com.yevhenii.cluster.planner.server.graphs.GraphShow._

import scala.util.Random

object GraphRandomizer extends LazyLogging {

  val rand = new Random()
  val MaxNumberOfTries = 20
  val MaxNumberOfRetriesOfFittingEdges = 1000
  val Epsilon = 0.001

  def createRandomOrientedGraph(parameters: GraphParameters): OrientedGraph = {
    val nodes = createRandomNodes(parameters)
    val edges = createRandomEdges(nodes, parameters)

    fitEdges(nodes, edges, parameters)
  }

  private def createRandomNodes(parameters: GraphParameters): List[Node] = {
    (1 to parameters.numberOfNodes)
      .map(i => (i, randomIntBetweenInclusive(parameters.minimalNodeWeight, parameters.maximumNodeWeight)))
      .map { case (index, weight) => Node(s"$index", s"$index-$weight", weight) }
      .toList
  }

  private def createRandomEdges(nodes: List[Node], parameters: GraphParameters): List[OrientedEdge] = {
    val numberOfEdges = randomIntBetweenInclusive((nodes.size * 0.8).toInt, nodes.size * 2)

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
    OrientedEdge(s"$source-$target", s"[$weight]", weight, source.toString, target.toString)
  }

  // todo: make more readable
  private def fitEdges(nodes: List[Node], edges: List[OrientedEdge], parameters: GraphParameters, retry: Int = 0): OrientedGraph = {
    val targetCorrelation = parameters.connectivity

    val nodeWeights = nodes.map(_.weight).sum
    val edgeWeights = edges.map(_.weight).sum

    val correlation = GraphOps.correlationOfConnections(nodeWeights, edgeWeights)

    if (retry > MaxNumberOfRetriesOfFittingEdges) {
      logger.warn(s"Could not fit edges fine, exhausted number of retries, target correlation = $targetCorrelation, current corraltion = $correlation")
      val graph = OrientedGraph(nodes ::: edges)
      logger.info(s"\n${Show[OrientedGraph].show(graph)}\n")

      graph
    } else {

      if (math.abs(targetCorrelation - correlation) <= Epsilon) {
        OrientedGraph(nodes ::: edges)
      } else {

        val shuffledEdges = Random.shuffle(edges)

        if (correlation < targetCorrelation) {
          val correlationWithoutMaxEdge = GraphOps.correlationOfConnections(nodeWeights, edgeWeights - parameters.maximumEdgeWeight)

          if (math.abs(targetCorrelation - correlationWithoutMaxEdge) < math.abs(targetCorrelation - correlation)) {

            fitEdges(nodes, edges.tail, parameters, retry + 1)
          } else {

            val reducedEdgeWeight = shuffledEdges.head.weight - 1

            val updatedEdges =
              if (reducedEdgeWeight < parameters.minimalEdgeWeight) shuffledEdges.tail
              else shuffledEdges.head.copy(weight = reducedEdgeWeight) :: shuffledEdges.tail

            fitEdges(nodes, updatedEdges, parameters, retry + 1)
          }
        } else {

          val correlationWithNewEdge = GraphOps.correlationOfConnections(nodeWeights, edgeWeights + parameters.minimalEdgeWeight)

          if (math.abs(targetCorrelation - correlationWithNewEdge) < math.abs(targetCorrelation - correlation)) {

            val maxNumberOfEdges = (0 until parameters.numberOfNodes).sum

            val newEdgeOpt = if (edges.size < maxNumberOfEdges) {
              tryToCreateEdge(nodes, edges, randomIntBetweenInclusive(parameters.minimalEdgeWeight, parameters.maximumEdgeWeight))
            } else {
              None
            }

            newEdgeOpt match {
              case Some(edge) =>
                fitEdges(nodes, edge :: shuffledEdges, parameters, retry + 1)
              case None =>
                val graph = OrientedGraph(nodes ::: edges)
//                logger.warn(s"Cannot create edge for graph:\n${Show[OrientedGraph].show(graph)}\n")

                shuffledEdges.find(_.weight < parameters.maximumEdgeWeight) match {
                  case Some(_) =>
                    val updatedEdges = shuffledEdges.map(edge => edge.copy(weight = math.min(edge.weight + 1, parameters.maximumEdgeWeight)))
                    fitEdges(nodes, updatedEdges, parameters, retry + 1)
                  case None =>
                    val graph = OrientedGraph(nodes ::: edges)
                    logger.warn(s"Cannot increment edge for graph:\n${Show[OrientedGraph].show(graph)}\n")
                    graph
                }
            }
          } else {

            val shouldBeIncremented = shuffledEdges.find(edge => edge.weight < parameters.maximumEdgeWeight)
            shouldBeIncremented match {
              case None =>
                val graph = OrientedGraph(nodes ::: edges)
                logger.warn(s"Cannot increment edge for graph:\n${Show[OrientedGraph].show(graph)}\n")
                graph

              case Some(edge) =>
                val updatedEdges = edge.copy(weight = edge.weight + 1) :: shuffledEdges.filterNot(_ == edge)
                fitEdges(nodes, updatedEdges, parameters, retry + 1)
            }

            // todo: faster but evristic
//            val updatedEdges = shuffledEdges.head.copy(weight = math.min(shuffledEdges.head.weight + 1, parameters.maximumEdgeWeight)) :: shuffledEdges.tail
//
//            fitEdges(nodes, updatedEdges, parameters, retry + 1)
          }
        }
      }
    }
  }

  private def randomIntBetweenInclusive(min: Int, max: Int): Int = {
    rand.nextInt(max - min + 1) + min
  }
}
