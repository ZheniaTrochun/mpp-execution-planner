package com.yevhenii.cluster.planner.server.modeling

import cats.{Monoid, Traverse}
import cats.data.Writer
import cats.instances.list._
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, OrientedGraph}
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._
import com.yevhenii.cluster.planner.server.modeling.ComputingNode.Log

import scala.collection.mutable

object ExecutionPlanner {

  val MaxIterationNumber = 10000

  type PartialRes = (ComputingNode, Option[Transfer], Option[Work])

  def planExecutionByConnectivity(
    systemGraph: NonOrientedGraph,
    taskGraph: OrientedGraph,
    queueCreator: OrientedGraph => List[Node]
  ): List[NodeLog] = {

    val queueOfNodes = orderOfComputingNodes(systemGraph)
    val queueOfTasks = queueCreator(taskGraph)

    val computingNodes = queueOfNodes.map(ComputingNode.init)

    val firstIteration = initComputing(computingNodes, queueOfTasks, taskGraph)

//    println(firstIteration)

    val resultWriter = loopByConnectivity(Writer.apply(List.empty, List.empty), systemGraph, taskGraph, List.empty, List.empty, 0)
    val (log, resultState) = resultWriter.run

    log
  }

  private def loopByConnectivity(
    prevResult: Writer[List[NodeLog], List[ComputingNode]],
    systemGraph: NonOrientedGraph,
    taskGraph: OrientedGraph,
    alreadyComputed: List[Node],
    transferred: List[Transfer],
    iteration: Int
  ): Writer[List[NodeLog], List[ComputingNode]] = {
    if (iteration == MaxIterationNumber) {
      prevResult
    } else {
      val (log, prev) = prevResult.run

      val newRes: Writer[Log, List[PartialRes]] = writerTraverse(prev)(node => node.tick(iteration))
      val withLog: Writer[List[Log], List[PartialRes]] = newRes.mapWritten(newLog => newLog :: log)

      val (allLog, partialResults) = withLog.run

      val newComputed = partialResults
        .flatMap { case (_, _, finishedWork) => finishedWork }
        .map(_.task) ::: alreadyComputed

      val transferredThisTick = partialResults.flatMap { case (_, finishedTransfer, _) => finishedTransfer }
      val (finishedTransfer, unfinishedTransfer) = transferredThisTick.partition(t => t.to == t.finalTarget)

      val newTransferred = finishedTransfer ::: transferred

      // todo: schedule new transfer

      val computingNodes = partialResults.map { case (res, _, _) => res }
      val mapOfComputingNodes = mutable.Map(computingNodes.map(node => node.id -> node): _*)

      // todo: can be implemented via Vector
      unfinishedTransfer.foreach { tr =>
        val nextEdge = systemGraph.findShortestPath(tr.to, tr.finalTarget, tr.amount).head
        val nextNode = if (nextEdge.target == tr.to) nextEdge.source else nextEdge.target

        val newTransfer = tr.copy(from = tr.to, to = nextNode, linkSize = nextEdge.weight)
        mapOfComputingNodes(tr.to) = mapOfComputingNodes(tr.to).addTransfer(newTransfer)
      }

      val withTransferScheduled = computingNodes.map(node => mapOfComputingNodes(node.id))
      // todo: schedule new tasks

      val currResults = partialResults.map { case (res, _, _) => res }

      val currRes = Writer.apply(allLog, currResults)

      loopByConnectivity(currRes, systemGraph, taskGraph, newComputed, newTransferred, iteration + 1)
    }
  }

  // todo: move to utility class
  private def writerSequence[L: Monoid, R](subj: List[Writer[L, R]]): Writer[L, List[R]] = {
    type λ[T] = Writer[L, T]
    Traverse[List].sequence[λ, R](subj)
  }

  private def writerTraverse[L: Monoid, A, B](subj: List[A])(f: A => Writer[L, B]): Writer[L, List[B]] = {
    type λ[T] = Writer[L, T]
    Traverse[List].traverse[λ, A, B](subj)(f)
  }

  private def orderOfComputingNodes(nonOrientedGraph: NonOrientedGraph): List[Node] =
    nonOrientedGraph.nodes.sortBy(nonOrientedGraph.connectivityOfNode)(Ordering.Int.reverse)

  private def isTaskReadyToBeComputed(task: Node, taskGraph: OrientedGraph, computedNodes: List[Node]): Boolean = {
    val computedNodeIds = computedNodes.map(_.id)
    val parents = taskGraph.edges.filter(edge => edge.target == task.id).map(_.source)

    parents.forall(computedNodeIds.contains)
  }

  private def initComputing(
    computingNodesQueue: List[ComputingNode],
    queueOfTasks: List[Node],
    taskGraph: OrientedGraph
  ): List[ComputingNode] = {

    queueOfTasks.filter(isTaskReadyToBeComputed(_, taskGraph, List.empty))
      .take(computingNodesQueue.size)
      .map(task => Work(0, task.weight, task))
      .zip(computingNodesQueue)
      .map { case (work, node) => node.addComputeTask(work) }
  }
}
