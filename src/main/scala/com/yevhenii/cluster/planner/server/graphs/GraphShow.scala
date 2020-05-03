package com.yevhenii.cluster.planner.server.graphs

import cats.Show
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedEdge, NonOrientedGraph, OrientedEdge, OrientedGraph}

object GraphShow {

  implicit val nodeShow: Show[Node] = (node: Node) => node.label
  implicit val orientedEdgeShow: Show[OrientedEdge] = (edge: OrientedEdge) => s"--${edge.label}-->"
  implicit val nonOrientedEdgeShow: Show[NonOrientedEdge] = (edge: NonOrientedEdge) => s"--${edge.label}--"

  implicit val orientedGraph: Show[OrientedGraph] = (graph: OrientedGraph) =>
    showGraph[OrientedGraph, OrientedEdge, Node](
      graph,
      g => g.edges,
      edge => (graph.nodesMap(edge.source), graph.nodesMap(edge.target))
    )

  implicit val nonOrientedGraph: Show[NonOrientedGraph] = (graph: NonOrientedGraph) =>
    showGraph[NonOrientedGraph, NonOrientedEdge, Node](
      graph,
      g => g.edges,
      edge => (graph.nodesMap(edge.source), graph.nodesMap(edge.target))
    )

//  implicit val orientedGraph: Show[OrientedGraph] = (graph: OrientedGraph) => {
//    graph.edges
//      .map { edge =>
//        val sourceNode = graph.nodesMap(edge.source)
//        val targetNode = graph.nodesMap(edge.target)
//
//        s"${Show[Node].show(sourceNode)} ${Show[OrientedEdge].show(edge)} ${Show[Node].show(targetNode)}"
//      }
//      .mkString("\n")
//  }

  private def showGraph[G, E: Show, N: Show](graph: G, edges: G => List[E], getConnectionsForEdge: E => (N, N)): String = {
    edges(graph)
      .map { edge =>
        val (sourceNode, targetNode) = getConnectionsForEdge(edge)

        s"${Show[N].show(sourceNode)} ${Show[E].show(edge)} ${Show[N].show(targetNode)}"
      }
      .mkString("\n")
  }
}
