package com.yevhenii.cluster.planner.server.model

import com.yevhenii.cluster.planner.server.dto.{Data, GraphEntry => DtoGraphEntry}
import com.yevhenii.cluster.planner.server.graphs.GraphOps

sealed trait GraphEntry

sealed trait OrientedGraphEntry extends GraphEntry
sealed trait NonOrientedGraphEntry extends GraphEntry

case class Node(
  id: String,
  label: String,
  weight: Int
) extends OrientedGraphEntry with NonOrientedGraphEntry

sealed trait Edge extends GraphEntry

case class OrientedEdge(
  id: String,
  label: String,
  weight: Int,
  source: String,
  target: String
) extends OrientedGraphEntry with Edge

case class NonOrientedEdge(
  id: String,
  label: String,
  weight: Int,
  source: String,
  target: String
) extends NonOrientedGraphEntry with Edge

object GraphEntry {
  type EdgeApply[E] = (String, String, Int, String, String) => E
  type NodeApply[T] = (String, String, Int) => T

  def from[T <: GraphEntry](entries: List[DtoGraphEntry])(createNode: NodeApply[T], createEdge: EdgeApply[T]): List[T] = {
    entries.map(_.data)
      .foldLeft(List.empty[T]) { (acc, entry) =>
        entry match {
          case Data(id, label, weight, None, None) =>
            createNode(id, label, weight.toInt) :: acc
          case Data(id, label, weight, Some(source), Some(target)) =>
            createEdge(id, label, weight.toInt, source, target) :: acc
          case _ => acc
        }
      }
  }
}

case class OrientedGraph(entries: List[OrientedGraphEntry]) {
  val edges = entries.collect { case e: OrientedEdge => e }
  val nodes = entries.collect { case n: Node => n }
  val nodesMap = nodes.view.map(n => (n.id, n)).toMap

  val isCorrect: Boolean = GraphOps.checkGraphForCycles(this)

  lazy val initialVertices = GraphOps.findInitialVertices(this)
  lazy val terminalVertices = GraphOps.findTerminalVertices(this)
}

case class NonOrientedGraph(entries: List[NonOrientedGraphEntry]) {
  val edges = entries.collect { case e: NonOrientedEdge => e }
  val nodes = entries.collect { case n: Node => n }
  val nodesMap = nodes.view.map(n => (n.id, n)).toMap

  val isCorrect: Boolean = GraphOps.checkGraphForConnectivity(this)
}

object OrientedGraphEntry {
  def apply(entries: List[DtoGraphEntry]): List[OrientedGraphEntry] =
    GraphEntry.from(entries)(Node.apply, OrientedEdge.apply)

  def from(entries: List[DtoGraphEntry]): List[OrientedGraphEntry] = apply(entries)
}

object NonOrientedGraphEntry {
  def apply(entries: List[DtoGraphEntry]): List[NonOrientedGraphEntry] =
    GraphEntry.from(entries)(Node.apply, NonOrientedEdge.apply)

  def from(entries: List[DtoGraphEntry]): List[NonOrientedGraphEntry] = apply(entries)
}