package com.yevhenii.cluster.planner.server.model

import com.yevhenii.cluster.planner.server.dto.{Data, GraphEntry}

sealed trait Graph

sealed trait OrientedGraph extends Graph
sealed trait NonOrientedGraph extends Graph

case class Node(
  id: String,
  label: String,
  weight: String
) extends OrientedGraph with NonOrientedGraph

sealed trait Edge extends Graph

case class OrientedEdge(
  id: String,
  label: String,
  weight: String,
  source: String,
  target: String
) extends OrientedGraph with Edge

case class NonOrientedEdge(
  id: String,
  label: String,
  weight: String,
  source: String,
  target: String
) extends NonOrientedGraph with Edge

object Graph {
  type EdgeApply[E] = (String, String, String, String, String) => E
  type NodeApply[T] = (String, String, String) => T

  def from[T <: Graph](entries: List[GraphEntry])(createNode: NodeApply[T], createEdge: EdgeApply[T]): List[T] = {
    entries.map(_.data)
      .foldLeft(List.empty[T]) { (acc, entry) =>
        entry match {
          case Data(id, label, weight, None, None) =>
            createNode(id, label, weight) :: acc
          case d @ Data(id, label, weight, Some(source), Some(target)) =>
            createEdge(id, label, weight, source, target) :: acc
        }
      }
  }
}

object OrientedGraph {
  def apply(entries: List[GraphEntry]): List[OrientedGraph] =
    Graph.from(entries)(Node.apply, OrientedEdge.apply)

  def from(entries: List[GraphEntry]): List[OrientedGraph] = apply(entries)
}

object NonOrientedGraph {
  def apply(entries: List[GraphEntry]): List[NonOrientedGraph] =
    Graph.from(entries)(Node.apply, NonOrientedEdge.apply)

  def from(entries: List[GraphEntry]): List[NonOrientedGraph] = apply(entries)
}