package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, OrientedGraph}

object Queues {

  sealed trait QueueCreator extends (OrientedGraph => List[Node])

  val Algorithms: List[QueueCreator] = List(CriticalPathBased, NodesOnCriticalPathBased, ConnectivityBased)

  object CriticalPathBased extends QueueCreator {

    override def apply(taskGraph: OrientedGraph): List[Node] = applyExtended(taskGraph).map(_._1)

    def applyExtended(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
      if (taskGraph.nodes.isEmpty || !taskGraph.isCorrect) {
        List.empty
      } else {
        taskGraph.nodes
          .view
          .map(node => (node, taskGraph.criticalPath(node)))
          .collect { case (node, Some(path)) => (node, path) }
          .sortBy { case (node, path) => path.map(_.weight).sum } (Ordering.Int.reverse)
          .toList
      }
    }
  }

  object NodesOnCriticalPathBased extends QueueCreator {

    override def apply(taskGraph: OrientedGraph): List[Node] = applyExtended(taskGraph).map(_._1)

    def applyExtended(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
      if (taskGraph.nodes.isEmpty || !taskGraph.isCorrect) {
        List.empty
      } else {
        val criticalPath = taskGraph.criticalPathByNodesCount
        val startOfQueue = criticalPath.zipWithIndex.map { case (elem, index) => elem -> criticalPath.drop(index) }

        val nodesNotOnCriticalPath = taskGraph.nodes
          .filterNot(criticalPath.contains)

        val restOfQueue = nodesNotOnCriticalPath.view
          .map(node => (node, taskGraph.criticalPathByNodesCount(node)))
          .collect { case (node, Some(path)) => (node, path) }
          .sortBy { case (node, path) => path.length } (Ordering.Int.reverse)
          .toList

        startOfQueue ::: restOfQueue
      }
    }
  }

  object ConnectivityBased extends QueueCreator {

    override def apply(taskGraph: OrientedGraph): List[Node] = applyExtended(taskGraph).map(_._1)

    def apply(systemGraph: NonOrientedGraph): List[Node] = {
      if (systemGraph.nodes.isEmpty || !systemGraph.isCorrect) {
        List.empty
      } else {
        systemGraph.nodes
          .map { node =>
            val connectivity = systemGraph.connectivityOfNode(node)
            (node, connectivity)
          }
          .sortBy(_._2)(Ordering.Int.reverse)
          .map(_._1)
      }
    }

    def applyExtended(taskGraph: OrientedGraph): List[(Node, (Int, Int))] = {
      if (taskGraph.nodes.isEmpty || !taskGraph.isCorrect) {
        List.empty
      } else {
        taskGraph.nodes
          .map { node =>
            val connectivity = taskGraph.connectivityOfNode(node)
            val criticalPath = taskGraph.criticalPathByNodesCount(node)
            (node, (connectivity, criticalPath.map(_.length).getOrElse(0)))
          }
          .sortBy(_._2)(Ordering.Tuple2(Ordering.Int.reverse, Ordering.Int.reverse))
      }
    }
  }

  implicit val queuesAlgoShow: Show[QueueCreator] = {
    case NodesOnCriticalPathBased => "Algorithm 5"
    case ConnectivityBased => "Algorithm 10"
    case CriticalPathBased => "Algorithm 3"
  }
}
