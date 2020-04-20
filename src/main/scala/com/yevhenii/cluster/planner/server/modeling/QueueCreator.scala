package com.yevhenii.cluster.planner.server.modeling

import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._
import com.yevhenii.cluster.planner.server.model.{Node, OrientedGraph}

object QueueCreator {

  def createQueueBasedOnCriticalPath(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
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

  def createQueueBasedOnNodesCountOnCriticalPath(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
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

  def createQueueBasedOnNodesConnectivity(taskGraph: OrientedGraph): List[(Node, (Int, Int))] = {
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
