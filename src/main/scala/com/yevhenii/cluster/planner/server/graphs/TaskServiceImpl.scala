package com.yevhenii.cluster.planner.server.graphs
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.dto.{Graphs, Task, TaskInit}
import com.yevhenii.cluster.planner.server.entity.GraphsEntity

class TaskServiceImpl(taskRepository: TaskRepository) extends TaskService with LazyLogging {

  override def initTask(task: TaskInit): IO[TaskId] =
    IO(logger.info(s"Initializing task [${task.name}]"))
      .flatMap(_ => taskRepository.initTask(task))

  override def deleteTask(id: TaskId): IO[Either[String, Unit]] =
    IO(logger.info(s"Removing all data for taskId [$id]"))
      .flatMap(_ => taskRepository.deleteTask(id))

  override def getTasks(): IO[List[Task]] =
    IO(logger.info("Listing all existing tasks"))
      .flatMap(_ => taskRepository.getTasks())
      .map(_.map(_.toTask))

  override def getGraphs(id: TaskId): IO[Option[Graphs]] =
    IO(logger.info(s"Retrieving graphs for taskId [$id"))
      .flatMap(_ => taskRepository.getGraphs(id))
      .map(_.map(_.toGraphs))

  override def updateGraphs(id: TaskId, graphs: Graphs): IO[Either[String, Unit]] =
    IO(logger.info(s"Updating graphs for taskId [$id]"))
      .flatMap(_ => taskRepository.updateGraphs(GraphsEntity.from(id, graphs)))

  override def init(): IO[Unit] =
    IO(logger.info("Initializing default start data"))
      .flatMap(_ => taskRepository.init())
}
