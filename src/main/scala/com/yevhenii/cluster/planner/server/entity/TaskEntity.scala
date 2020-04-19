package com.yevhenii.cluster.planner.server.entity

import com.yevhenii.cluster.planner.server.dto.Task
import org.mongodb.scala.bson.ObjectId

case class TaskEntity(_id: ObjectId, name: String) {
  def toTask: Task = Task(_id.toString, name)
}
