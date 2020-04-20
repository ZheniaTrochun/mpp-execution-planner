package com.yevhenii.cluster.planner.server.modeling

import com.yevhenii.cluster.planner.server.graphs.GraphOps
import com.yevhenii.cluster.planner.server.model.{Node, OrientedGraph}

object QueueCreator {

  def createQueueBasedOnCriticalPath(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
    if (taskGraph.nodes.isEmpty || !GraphOps.checkGraphForCycles(taskGraph)) {
      Nil
    } else {
      taskGraph.nodes
        .view
        .map(node => (node, GraphOps.findCriticalPathUnsafe(taskGraph, node)))
        .collect { case (node, Some(path)) => (node, path) }
        .sortBy { case (node, path) => path.map(_.weight).sum } (Ordering.Int.reverse)
        .toList
    }
  }

  def createQueueBasedOnNodesCountOnCriticalPath(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
    if (taskGraph.nodes.isEmpty || !GraphOps.checkGraphForCycles(taskGraph)) {
      Nil
    } else {
      val criticalPath = GraphOps.findCriticalPathByNodesCount(taskGraph)
      val startOfQueue = criticalPath.zipWithIndex.map { case (elem, index) => elem -> criticalPath.drop(index) }

      val nodesNotOnCriticalPath = taskGraph.nodes
        .filterNot(criticalPath.contains)

      val restOfQueue = nodesNotOnCriticalPath.view
        .map(node => (node, GraphOps.findCriticalPathByNodesCountUnsafe(taskGraph, node)))
        .collect { case (node, Some(path)) => (node, path) }
        .sortBy { case (node, path) => path.length } (Ordering.Int.reverse)
        .toList

      startOfQueue ::: restOfQueue
    }
  }

  def createQueueBasedOnNodesConnectivity(taskGraph: OrientedGraph): List[(Node, List[Node])] = {
    if (taskGraph.nodes.isEmpty || !GraphOps.checkGraphForCycles(taskGraph)) {
      Nil
    } else {
//      val nodesByCo
      Nil
    }
  }
}
