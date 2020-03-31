package com.yevhenii.claster.planner.server.models

import com.yevhenii.claster.planner.server.models.Task.TaskId

case class Task(id: TaskId, name: String)

case class TaskInit(name: String)

object Task {
  type TaskId = String
}
