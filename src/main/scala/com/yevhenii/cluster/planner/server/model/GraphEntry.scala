package com.yevhenii.cluster.planner.server.model

import com.yevhenii.cluster.planner.server.dto.{Data, GraphEntry => DtoGraphEntry}
import com.yevhenii.cluster.planner.server.graphs.GraphOps

sealed trait GraphEntry

sealed trait OrientedGraphEntry extends GraphEntry
sealed trait NonOrientedGraphEntry extends GraphEntry

case class Node(
  id: String,
  weight: Int
) extends OrientedGraphEntry with NonOrientedGraphEntry {
  def label: String = s"$id - [$weight]"
}

sealed trait Edge extends GraphEntry {
  val id: String
  val weight: Int
  val source: String
  val target: String

  def label: String
}

sealed trait Graph[E <: GraphEntry] {
  val entries: List[E]
  val nodes: List[Node]
  val edges: List[E with Edge]
  val nodesMap: Map[String, Node]

  val isCorrect: Boolean
}

case class OrientedEdge(
  id: String,
  weight: Int,
  source: String,
  target: String
) extends OrientedGraphEntry with Edge {
  def label: String = s"[$weight]"
}

case class NonOrientedEdge(
  id: String,
  weight: Int,
  source: String,
  target: String
) extends NonOrientedGraphEntry with Edge {
  def label: String = s"[$weight]"
}

object GraphEntry {
  type EdgeApply[E] = (String, Int, String, String) => E
  type NodeApply[T] = (String, Int) => T

  def from[T <: GraphEntry](entries: List[DtoGraphEntry])(createNode: NodeApply[T], createEdge: EdgeApply[T]): List[T] = {
    entries.map(_.data)
      .foldLeft(List.empty[T]) { (acc, entry) =>
        entry match {
          case Data(id, _, weight, None, None) =>
            createNode(id, weight.toInt) :: acc
          case Data(id, _, weight, Some(source), Some(target)) =>
            createEdge(id, weight.toInt, source, target) :: acc
          case _ => acc
        }
      }
  }
}

case class OrientedGraph(entries: List[OrientedGraphEntry]) extends Graph[OrientedGraphEntry] {
  val edges: List[OrientedEdge] = entries.collect { case e: OrientedEdge => e }
  val nodes: List[Node] = entries.collect { case n: Node => n }
  val nodesMap: Map[String, Node] = nodes.view.map(n => (n.id, n)).toMap

  val isCorrect: Boolean = GraphOps.checkGraphForCycles(this)

  lazy val initialVertices = GraphOps.findInitialVertices(this)
  lazy val terminalVertices = GraphOps.findTerminalVertices(this)
}

case class NonOrientedGraph(entries: List[NonOrientedGraphEntry]) extends Graph[NonOrientedGraphEntry] {
  val edges: List[NonOrientedEdge] = entries.collect { case e: NonOrientedEdge => e }
  val nodes: List[Node] = entries.collect { case n: Node => n }
  val nodesMap: Map[String, Node] = nodes.view.map(n => (n.id, n)).toMap

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