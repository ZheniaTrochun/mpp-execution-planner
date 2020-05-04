package com.yevhenii.cluster.planner.server.dto

import com.yevhenii.cluster.planner.server.model.{Node, OrientedGraph}

case class Graphs(taskGraph: List[GraphEntry], systemGraph: List[GraphEntry])

case class Data(
  id: String,
  label: String,
  weight: String,
  source: Option[String],
  target: Option[String]
)

object Data {
  def from(node: Node): Data = Data(node.id, node.label, node.weight.toString, None, None)
}

case class Position(x: Int, y: Int)

case class GraphEntry(
  data: Data,
  position: Position,
  group: String,
  removed: Boolean,
  selected: Boolean,
  selectable: Boolean,
  locked: Boolean,
  grabbable: Boolean,
  pannable: Boolean,
  classes: String
)

object GraphEntry {
  def from(orientedGraph: OrientedGraph): List[GraphEntry] = {
    val formattedNodes = orientedGraph.nodes.map(node => GraphEntry(
      Data(node.id, node.label, node.weight.toString, None, None),
      Position((node.id.toInt % 4 - 1) * 200, (node.id.toInt / 4 + 1) * 200),
      "nodes",
      removed = false,
      selected = false,
      selectable = true,
      locked = false,
      grabbable = true,
      pannable = false,
      ""
    ))

    val formattedEdges = orientedGraph.edges.map(edge => GraphEntry(
      Data(edge.id, edge.label, edge.weight.toString, Some(edge.source), Some(edge.target)),
      Position(0, 0),
      "edges",
      removed = false,
      selected = false,
      selectable = true,
      locked = false,
      grabbable = true,
      pannable = true,
      ""
    ))

    formattedNodes ::: formattedEdges
  }
}