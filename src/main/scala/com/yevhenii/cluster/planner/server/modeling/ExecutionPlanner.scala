package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, OrientedGraph}
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._

import scala.annotation.tailrec

object ExecutionPlanner extends LazyLogging {

  val MaxIterationNumber = 1000

  def planExecutionByConnectivity(
    systemGraph: NonOrientedGraph,
    taskGraph: OrientedGraph,
    queueCreator: OrientedGraph => List[Node]
  ): GhantDiagram = {

    val queueOfNodes = orderOfComputingNodes(systemGraph)
    val queueOfTasks = queueCreator(taskGraph)

//    val nodesLog = Show[List[Node]].show(queueOfNodes)
//    val tasksLog = Show[List[Node]].show(queueOfTasks)

//    logger.info(s"queue of nodes: $nodesLog")
//    logger.info(s"queue of tasks: $tasksLog")

    val (diagram, scheduled) = initComputing(queueOfNodes, queueOfTasks, taskGraph, systemGraph)

    loopOfPlanning(1, List.empty, scheduled, queueOfTasks, queueOfNodes, taskGraph, diagram)
  }

  @tailrec private def loopOfPlanning(
    tact: Int,
    finishedTasks: List[Node],
    scheduledTasks: List[Node],
    taskQueue: List[Node],
    nodesQueue: List[Node],
    taskGraph: OrientedGraph,
    diagram: GhantDiagram
  ): GhantDiagram = {

    if (finishedTasks.size == taskQueue.size || tact == MaxIterationNumber) {
      diagram
    } else {

      val recentlyFinished = diagram.findRecentlyFinished(tact)
//      logger.info(s"recently finished: ${recentlyFinished.map(_.id).mkString(", ")}")

      val newComputed = recentlyFinished ::: finishedTasks
      val readyNodes = findTasksReadyToBeComputed(taskGraph, taskQueue, scheduledTasks, newComputed)

//      logger.info(s"ready to be computed: ${readyNodes.map(_.id).mkString(", ")}")
      val freeProcessors = diagram.freeProcessorIds(tact)

      val newScheduled = readyNodes.zip(freeProcessors)
        .map { case (node, processorId) =>

          val parentData = taskGraph.edges.filter(_.target == node.id)
          val whenEverythingIsTransferred = parentData
            .flatMap { edge =>
              val whereWasComputed = diagram.whereWasComputed(edge.source)
              whereWasComputed.map(n => edge -> n)
            }
            .foldLeft(tact) { (startAt, whereWasComputedPair) =>
              val (edge, whereWasComputed) = whereWasComputedPair
              diagram.transferTo(whereWasComputed, processorId, edge, startAt)
            }

          diagram.schedule(processorId, node, whenEverythingIsTransferred)
          node
        }

//      val log = Show[GhantDiagram].show(diagram)
//      logger.info(s"\n$log")
//      logger.info("")
//      logger.info("")

      loopOfPlanning(tact + 1, newComputed, newScheduled ::: scheduledTasks, taskQueue, nodesQueue, taskGraph, diagram)
    }
  }

  private def orderOfComputingNodes(nonOrientedGraph: NonOrientedGraph): List[Node] =
    nonOrientedGraph.nodes.sortBy(nonOrientedGraph.connectivityOfNode)(Ordering.Int.reverse)

  private def isTaskReadyToBeComputed(task: Node, taskGraph: OrientedGraph, computedNodes: List[Node]): Boolean = {
    val computedNodeIds = computedNodes.map(_.id)
    val parents = taskGraph.edges.filter(edge => edge.target == task.id).map(_.source)

    parents.forall(computedNodeIds.contains)
  }

  private def findTasksReadyToBeComputed(taskGraph: OrientedGraph, taskQueue: List[Node], scheduled: List[Node], computedNodes: List[Node]): List[Node] = {
    taskQueue
      .view
      .filterNot(t => computedNodes.exists(_.id == t.id))
      .filterNot(t => scheduled.exists(_.id == t.id))
      .filter(isTaskReadyToBeComputed(_, taskGraph, computedNodes))
      .toList
  }

  private def initComputing(
    computingNodesQueue: List[Node],
    queueOfTasks: List[Node],
    taskGraph: OrientedGraph,
    systemGraph: NonOrientedGraph
  ): (GhantDiagram, List[Node]) = {

    val diagram = new GhantDiagram(systemGraph)

    val scheduled = queueOfTasks
      .filter(isTaskReadyToBeComputed(_, taskGraph, List.empty))
      .take(computingNodesQueue.size)
      .zip(computingNodesQueue)
      .map { case (work, node) =>
        diagram.schedule(node.id, work, 0)
        work
      }

    logger.info(s"initially scheduled: ${scheduled.map(_.id).mkString(", ")}")

    (diagram, scheduled)
  }

  implicit val nodeQueueShow: Show[List[Node]] = queue => queue.map(_.id).mkString(" -> ")
}
