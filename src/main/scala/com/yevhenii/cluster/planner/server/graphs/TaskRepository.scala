package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.models.{Graphs, Task, TaskInit}
import com.yevhenii.cluster.planner.server.models.Task.TaskId

trait TaskRepository {

  def initTask(task: TaskInit): IO[TaskId]
  def deleteTask(id: TaskId): IO[Either[String, Unit]]
  def getTasks(): IO[List[Task]]

  def getGraphs(id: TaskId): IO[Option[Graphs]]
  def updateGraphs(id: TaskId, graphs: Graphs): IO[Either[String, Unit]]

  def init(): IO[Unit]
}
