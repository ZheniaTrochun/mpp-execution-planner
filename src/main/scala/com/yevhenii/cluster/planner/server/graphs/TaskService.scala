package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.dto.{Graphs, Task, TaskInit}

trait TaskService {

  def initTask(task: TaskInit): IO[TaskId]
  def deleteTask(id: TaskId): IO[Either[String, Unit]]
  def getTasks(): IO[List[Task]]

  def getGraphs(id: TaskId): IO[Option[Graphs]]
  def updateGraphs(id: TaskId, graphs: Graphs): IO[Either[String, Unit]]

  def init(): IO[Unit]
}
