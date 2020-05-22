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

    loopOfPlanningByConnectivity(1, List.empty, scheduled, queueOfTasks, queueOfNodes, taskGraph, diagram)
  }

  def planExecutionByNeighbor(
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

    loopOfPlanningByNeighbor(1, List.empty, scheduled, queueOfTasks, queueOfNodes, taskGraph, systemGraph, diagram)
  }

  @tailrec private def loopOfPlanningByConnectivity(
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
      val freeProcessors = nodesQueue.filter(n => diagram.isFree(n.id, tact))

      val newScheduled = readyNodes.zip(freeProcessors)
        .map { case (node, processor) =>

          val parentData = taskGraph.edges.filter(_.target == node.id)
          val whenEverythingIsTransferred = parentData
            .flatMap { edge =>
              val whereWasComputed = diagram.whereWasComputed(edge.source)
              whereWasComputed.map(n => edge -> n)
            }
            .foldLeft(tact) { (startAt, whereWasComputedPair) =>
              val (edge, whereWasComputed) = whereWasComputedPair
              diagram.transferTo(whereWasComputed, processor.id, edge, startAt)
            }

          diagram.schedule(processor.id, node, whenEverythingIsTransferred)
          node
        }

//      val log = Show[GhantDiagram].show(diagram)
//      logger.info(s"\n$log")
//      logger.info("")
//      logger.info("")

      loopOfPlanningByConnectivity(tact + 1, newComputed, newScheduled ::: scheduledTasks, taskQueue, nodesQueue, taskGraph, diagram)
    }
  }

  @tailrec private def loopOfPlanningByNeighbor(
    tact: Int,
    finishedTasks: List[Node],
    scheduledTasks: List[Node],
    taskQueue: List[Node],
    nodesQueue: List[Node],
    taskGraph: OrientedGraph,
    systemGraph: NonOrientedGraph,
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
//      val freeProcessors = nodesQueue.filter(n => diagram.isFree(n.id, tact))
//      val freeProcessors = diagram.freeProcessorIds(tact)

      val numberOfFreeProcessors = diagram.freeProcessorIds(tact).size

      val newScheduled = readyNodes.take(numberOfFreeProcessors).map { node =>
        val parentData = taskGraph.edges.filter(_.target == node.id)
          .flatMap(edge => diagram.whereAndWhenWasComputed(edge.source).map(x => edge -> x))

        val processor = diagram.freeProcessorIds(tact)
          .map { processor =>
            val timeToTransfer = parentData
              .map { case (edge, (whereComputed, _)) =>
                (edge, systemGraph.findShortestPath(whereComputed, processor, edge.weight))
              }
              .map { case (edge, path) =>
                path.foldLeft(0)((acc, currEdge) => acc + math.ceil(edge.weight / currEdge.weight).toInt)
              }
              .sum

            (processor, timeToTransfer)
          }
          .minBy(_._2)
          ._1 // todo: this selection is pretty random, need to use processors queue

        val whenEverythingIsTransferred = if (parentData.isEmpty) {
          0
        } else {
          parentData
            .map { case (edge, (whereComputed, whenComputed)) =>
              diagram.transferTo(whereComputed, processor, edge, whenComputed + 1)  // todo: maybe should be "whenComputed"
            }
            .max
        }

        diagram.schedule(processor, node, whenEverythingIsTransferred.max(tact))
        node
      }

      //      val log = Show[GhantDiagram].show(diagram)
      //      logger.info(s"\n$log")
      //      logger.info("")
      //      logger.info("")

      loopOfPlanningByNeighbor(tact + 1, newComputed, newScheduled ::: scheduledTasks, taskQueue, nodesQueue, taskGraph, systemGraph, diagram)
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
