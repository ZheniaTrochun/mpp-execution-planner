package com.yevhenii.cluster.planner.server.modeling

import cats.Monoid

case class NodeLog(nodeId: String, states: List[(Int, State)])

object NodeLog {
  def empty(nodeId: String): NodeLog = NodeLog(nodeId, List.empty)

  implicit val logMonoid: Monoid[NodeLog] = new Monoid[NodeLog] {
    override def empty: NodeLog = NodeLog("", List.empty)

    override def combine(x: NodeLog, y: NodeLog): NodeLog = x match {
      case NodeLog("", xLog) => NodeLog(y.nodeId, xLog ::: y.states)
      case NodeLog(id, xLog) => NodeLog(id, xLog ::: y.states)
    }
  }
}
