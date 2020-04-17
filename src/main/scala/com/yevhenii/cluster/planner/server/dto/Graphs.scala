package com.yevhenii.cluster.planner.server.dto

case class Graphs(taskGraph: List[GraphEntry], systemGraph: List[GraphEntry])

case class Data(
  id: String,
  label: String,
  weight: String,
  source: Option[String],
  target: Option[String]
)

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
