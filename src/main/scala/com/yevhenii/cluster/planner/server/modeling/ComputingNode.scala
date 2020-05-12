package com.yevhenii.cluster.planner.server.modeling

import cats.data.Writer
import cats.kernel.{Monoid, Semigroup}
import com.yevhenii.cluster.planner.server.model.Node
import com.yevhenii.cluster.planner.server.modeling.ComputingNode.Log

import scala.collection.immutable.Queue

case class ComputingNode(
  id: String,
  computingPower: Int,
  state: State,
  transferQueue: Queue[Transfer] = Queue.empty,
  workQueue: Queue[Work] = Queue.empty
) {
//  import ComputingNode.logSemigroup

  def isComputing: Boolean = state match {
    case _: InWork => true
    case _ => false
  }

  def isTransferring: Boolean = state match {
    case _: InTransfer => true
    case _ => false
  }

  def addComputeTask(work: Work): ComputingNode = updated(state, transferQueue, work +: workQueue)

  def addTransfer(transfer: Transfer): ComputingNode = updated(state, transfer +: transferQueue, workQueue)

  // todo implement it
  def tick(time: Int): Writer[Log, (ComputingNode, Option[Transfer], Option[Work])] = ???

  private def updated(newState: State, newTransferQueue: Queue[Transfer], newWorkQueue: Queue[Work]): ComputingNode =
    ComputingNode(id, computingPower, newState, newTransferQueue, newWorkQueue)
}

object ComputingNode {
  type Log = NodeLog

//  implicit val logSemigroup: Semigroup[NodeLog] =
//    (x: NodeLog, y: NodeLog) => NodeLog(x.nodeId, x.states ::: y.states)

  def init(node: Node): ComputingNode =
    ComputingNode(node.id, node.weight, Waiting)
}