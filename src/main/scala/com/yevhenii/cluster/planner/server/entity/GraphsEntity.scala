package com.yevhenii.cluster.planner.server.entity

import com.yevhenii.cluster.planner.server.dto.{Data, GraphEntry, Graphs, Position}
import org.mongodb.scala.bson.ObjectId

case class GraphsEntity(_id: ObjectId, taskGraph: List[GraphEntryEntity], systemGraph: List[GraphEntryEntity]) {
  def toGraphs: Graphs = Graphs(
    taskGraph.map(_.toGraphsEntry),
    systemGraph.map(_.toGraphsEntry)
  )
}

case class DataEntity(
  id: String,
  label: String,
  weight: String,
  source: Option[String],
  target: Option[String]
) {
  def toData: Data = Data(id, label, weight, source, target)
}

case class PositionEntity(x: Double, y: Double) {
  def toPosition: Position = Position(x, y)
}

case class GraphEntryEntity(
  data: DataEntity,
  position: PositionEntity,
  group: String,
  removed: Boolean,
  selected: Boolean,
  selectable: Boolean,
  locked: Boolean,
  grabbable: Boolean,
  pannable: Boolean,
  classes: String
) {
  def toGraphsEntry: GraphEntry = GraphEntry(
    data = data.toData,
    position = position.toPosition,
    group = group,
    removed = removed,
    selected = selected,
    selectable = selectable,
    locked = locked,
    grabbable = grabbable,
    pannable = pannable,
    classes = classes
  )
}

// todo: Optics will fit here pretty good
object GraphsEntity {
  def from(id: String, dto: Graphs): GraphsEntity = GraphsEntity(
    new ObjectId(id),
    dto.taskGraph.map(GraphEntryEntity.from),
    dto.systemGraph.map(GraphEntryEntity.from)
  )
}

object GraphEntryEntity {
  def from(dto: GraphEntry): GraphEntryEntity = {
    GraphEntryEntity(
      data = DataEntity.from(dto.data),
      position = PositionEntity.from(dto.position),
      group = dto.group,
      removed = dto.removed,
      selected = dto.selected,
      selectable = dto.selectable,
      locked = dto.locked,
      grabbable = dto.grabbable,
      pannable = dto.pannable,
      classes = dto.classes
    )
  }
}

object DataEntity {
  def from(dtoData: Data): DataEntity = DataEntity(dtoData.id, dtoData.label, dtoData.weight, dtoData.source, dtoData.target)
}

object PositionEntity {
  def from(dtoPosition: Position): PositionEntity = PositionEntity(dtoPosition.x, dtoPosition.y)
}