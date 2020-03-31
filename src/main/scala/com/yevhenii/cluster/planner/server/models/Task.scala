package com.yevhenii.cluster.planner.server.models

import Task.TaskId

case class Task(id: TaskId, name: String)

case class TaskInit(name: String)

object Task {
  type TaskId = String
}
