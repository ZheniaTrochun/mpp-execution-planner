package com.yevhenii.cluster.planner.server.graphs

import com.yevhenii.cluster.planner.server.model.{GraphParameters, Node, OrientedEdge, OrientedGraph}

import scala.util.Random

object GraphRandomizer {

  val rand = new Random()
  val MaxNumberOfTries = 20

  def createRandomOrientedGraph(parameters: GraphParameters): OrientedGraph = {
    val nodes = createRandomNodes(parameters)
    val edges = createRandomEdges(nodes, parameters)

    OrientedGraph(nodes ::: edges)
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

  // todo
//  private def fitEdges(nodes: List[Node], edges: List[OrientedEdge], targetCorrelation: Double): OrientedGraph = {
//
//  }

  private def randomIntBetweenInclusive(min: Int, max: Int): Int = {
    rand.nextInt(max - min + 1) + min
  }
}
