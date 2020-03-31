package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.models.{Graphs, Task, TaskInit}
import com.yevhenii.cluster.planner.server.models.Task.TaskId

trait TaskRepository {

  def initTask(task: TaskInit): IO[TaskId]
  def getGraphs(id: TaskId): IO[Option[Graphs]]
  def deleteGraphs(id: TaskId): IO[Either[String, Unit]]
  def updateGraphs(id: TaskId, graphs: Graphs): IO[Either[String, Unit]]
  def getTasks(): IO[List[Task]]

  def init(): IO[Unit]
}
