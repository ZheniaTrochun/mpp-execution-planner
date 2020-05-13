package com.yevhenii.cluster.planner.server.modeling

import cats.{Monoid, Show, Traverse}
import cats.data.Writer
import cats.instances.list._
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, OrientedGraph}
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._
import com.yevhenii.cluster.planner.server.modeling.ComputingNode.Log

import scala.annotation.tailrec
import scala.collection.mutable

object ExecutionPlanner extends LazyLogging {

  val MaxIterationNumber = 10000
//
//  type PartialRes = (ComputingNode, Option[Transfer], Option[Work])
//
//  def planExecutionByConnectivity(
//    systemGraph: NonOrientedGraph,
//    taskGraph: OrientedGraph,
//    queueCreator: OrientedGraph => List[Node]
//  ): List[NodeLog] = {
//
//    val queueOfNodes = orderOfComputingNodes(systemGraph)
//    val queueOfTasks = queueCreator(taskGraph)
//
//    val computingNodes = queueOfNodes.map(ComputingNode.init)
//
//    val firstIteration = initComputing(computingNodes, queueOfTasks, taskGraph)
//
////    println(firstIteration)
//
//    val resultWriter = loopByConnectivity(Writer.apply(List.empty, List.empty), systemGraph, taskGraph, List.empty, List.empty, 0)
//    val (log, resultState) = resultWriter.run
//
//    log
//  }
//
//  private def loopByConnectivity(
//    prevResult: Writer[List[NodeLog], List[ComputingNode]],
//    systemGraph: NonOrientedGraph,
//    taskGraph: OrientedGraph,
//    taskQueue: List[Node],
//    alreadyComputed: List[Node],
//    transferred: List[Transfer],
//    iteration: Int
//  ): Writer[List[NodeLog], List[ComputingNode]] = {
//    if (iteration == MaxIterationNumber) {
//      prevResult
//    } else {
//      val (log, prev) = prevResult.run
//
//      val newRes: Writer[Log, List[PartialRes]] = writerTraverse(prev)(node => node.tick(iteration))
//      val withLog: Writer[List[Log], List[PartialRes]] = newRes.mapWritten(newLog => newLog :: log)
//
//      val (allLog, partialResults) = withLog.run
//
//      val newComputed = partialResults
//        .flatMap { case (_, _, finishedWork) => finishedWork }
//        .map(_.task) ::: alreadyComputed
//
//      val transferredThisTick = partialResults.flatMap { case (_, finishedTransfer, _) => finishedTransfer }
//      val (finishedTransfer, unfinishedTransfer) = transferredThisTick.partition(t => t.to == t.finalTarget)
//
//      val newTransferred = finishedTransfer ::: transferred
//
//      // todo: schedule new transfer
//
//      val computingNodes = partialResults.map { case (res, _, _) => res }
//      val mapOfComputingNodes = mutable.Map(computingNodes.map(node => node.id -> node): _*)
//
//      // todo: can be implemented via Vector
//      unfinishedTransfer.foreach { tr =>
//        val nextEdge = systemGraph.findShortestPath(tr.to, tr.finalTarget, tr.amount).head
//        val nextNode = if (nextEdge.target == tr.to) nextEdge.source else nextEdge.target
//
//        val newTransfer = tr.copy(from = tr.to, to = nextNode, linkSize = nextEdge.weight)
//        mapOfComputingNodes(tr.to) = mapOfComputingNodes(tr.to).addTransfer(newTransfer)
//      }
//
////      val withTransferScheduled: List[ComputingNode] = computingNodes.map(node => mapOfComputingNodes(node.id))
//
//      // todo: schedule new tasks
//      val tasksReadyToBeScheduled = taskQueue
//        .filterNot(newComputed.contains)
//        .filter(isTaskReadyToBeComputed(_, taskGraph, newComputed))
//
//      logger.info(s"all log: $allLog")
//      logger.info(s"tasksReady to be scheduled: $tasksReadyToBeScheduled")
//
//      val freeNodes = computingNodes.map(node => mapOfComputingNodes(node.id)).filterNot(_.isComputing)
//      tasksReadyToBeScheduled.zip(freeNodes).foreach { case (task, computingNode) =>
//        // find all finished transfer with desired data
//        val allNeededData = taskGraph.edges.filter(e => e.target == task.id)
//        if (newTransferred.count(t => t.dataForTaskId == task.id) == allNeededData.size) {
//          mapOfComputingNodes(computingNode.id) = computingNode.addComputeTask(Work(iteration, task.weight, task))
//        } else {
//
//        }
//      }
//
//      val currResults = partialResults.map { case (res, _, _) => res }
//
//      val currRes = Writer.apply(allLog, currResults)
//
//      loopByConnectivity(currRes, systemGraph, taskGraph, taskQueue, newComputed, newTransferred, iteration + 1)
//    }
//  }
//
//  // todo: move to utility class
//  private def writerSequence[L: Monoid, R](subj: List[Writer[L, R]]): Writer[L, List[R]] = {
//    type λ[T] = Writer[L, T]
//    Traverse[List].sequence[λ, R](subj)
//  }
//
//  private def writerTraverse[L: Monoid, A, B](subj: List[A])(f: A => Writer[L, B]): Writer[L, List[B]] = {
//    type λ[T] = Writer[L, T]
//    Traverse[List].traverse[λ, A, B](subj)(f)
//  }

  def planExecutionByConnectivity(
    systemGraph: NonOrientedGraph,
    taskGraph: OrientedGraph,
    queueCreator: OrientedGraph => List[Node]
  ): ComputingGhantDiagram = {

    val queueOfNodes = orderOfComputingNodes(systemGraph)
    val queueOfTasks = queueCreator(taskGraph)

    val nodesLog = Show[List[Node]].show(queueOfNodes)
    val tasksLog = Show[List[Node]].show(queueOfTasks)

    logger.info(s"queue of nodes: $nodesLog")
    logger.info(s"queue of tasks: $tasksLog")

    val (diagram, scheduled) = initComputing(queueOfNodes, queueOfTasks, taskGraph)

    val log = Show[ComputingGhantDiagram].show(diagram)
    logger.info(log)
    logger.info("")

    loopOfPlanning(1, List.empty, scheduled, queueOfTasks, queueOfNodes, taskGraph, diagram)
  }

  @tailrec private def loopOfPlanning(
    tact: Int,
    finishedTasks: List[Node],
    scheduledTasks: List[Node],
    taskQueue: List[Node],
    nodesQueue: List[Node],
    taskGraph: OrientedGraph,
    diagram: ComputingGhantDiagram
  ): ComputingGhantDiagram = {

    if (finishedTasks.size == taskQueue.size || tact == MaxIterationNumber) {
      diagram
    } else {

      val recentlyFinished = diagram.findRecentlyFinished(tact)
      val newComputed = recentlyFinished ::: finishedTasks
      val readyNodes = findTasksReadyToBeComputed(taskGraph, taskQueue, scheduledTasks ::: newComputed)
      val freeProcessors = diagram.freeProcessorIds(tact)

      val newScheduled = readyNodes.zip(freeProcessors).map { case (node, processorId) =>
        diagram.schedule(processorId, node, tact)
        node
      }

      val log = Show[ComputingGhantDiagram].show(diagram)
      logger.info(log)
      logger.info("")

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

  private def findTasksReadyToBeComputed(taskGraph: OrientedGraph, taskQueue: List[Node], computedNodes: List[Node]): List[Node] = {
    taskQueue
      .filterNot(computedNodes.contains)
      .filter(isTaskReadyToBeComputed(_, taskGraph, computedNodes))
  }

//  private def initComputing(
//    computingNodesQueue: List[ComputingNode],
//    queueOfTasks: List[Node],
//    taskGraph: OrientedGraph
//  ): List[ComputingNode] = {
//
//    queueOfTasks.filter(isTaskReadyToBeComputed(_, taskGraph, List.empty))
//      .take(computingNodesQueue.size)
//      .map(task => Work(0, task.weight, task))
//      .zip(computingNodesQueue)
//      .map { case (work, node) => node.addComputeTask(work) }
//  }

  private def initComputing(
    computingNodesQueue: List[Node],
    queueOfTasks: List[Node],
    taskGraph: OrientedGraph
  ): (ComputingGhantDiagram, List[Node]) = {

    val diagram = new ComputingGhantDiagram(computingNodesQueue)

    val scheduled = queueOfTasks
      .filter(isTaskReadyToBeComputed(_, taskGraph, List.empty))
      .take(computingNodesQueue.size)
      .zip(computingNodesQueue)
      .map { case (work, node) =>
        diagram.schedule(node.id, work, 0)
        node
      }

    (diagram, scheduled)
  }

  implicit val nodeQueueShow: Show[List[Node]] = queue => queue.map(_.id).mkString(" -> ")
}
