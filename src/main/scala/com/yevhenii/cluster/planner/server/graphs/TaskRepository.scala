package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.dto.TaskInit
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.entity.{GraphsEntity, TaskEntity}

trait TaskRepository {

  def initTask(task: TaskInit): IO[TaskId]
  def deleteTask(id: TaskId): IO[Either[String, Unit]]
  def getTasks(): IO[List[TaskEntity]]

  def getGraphs(id: TaskId): IO[Option[GraphsEntity]]
  def updateGraphs(graphs: GraphsEntity): IO[Either[String, Unit]]

  def init(): IO[Unit]
}
