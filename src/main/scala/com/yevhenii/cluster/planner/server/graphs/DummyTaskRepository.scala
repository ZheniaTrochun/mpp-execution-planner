package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.models.Task.TaskId
import com.yevhenii.cluster.planner.server.models.{Graphs, Task, TaskInit}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.yevhenii.cluster.planner.server.models
import com.yevhenii.cluster.planner.server.models.{Graphs, Task, TaskInit}

import scala.collection.JavaConverters._

class DummyTaskRepository extends TaskRepository {
  private val tasks = new ConcurrentHashMap[TaskId, Task]()
  private val graphs = new ConcurrentHashMap[TaskId, Graphs]()

  override def init(): IO[Unit] = IO {
    tasks.put("0cf70fbd-2350-46fb-9d01-e59502f6e187", models.Task("0cf70fbd-2350-46fb-9d01-e59502f6e187", "default task"))
    graphs.put("0cf70fbd-2350-46fb-9d01-e59502f6e187", Graphs(List(), List()))
  }

  override def initTask(taskInit: TaskInit): IO[TaskId] = IO {
    val id = UUID.randomUUID().toString
    val newTask = models.Task(id, taskInit.name)

    tasks.put(id, newTask)
    graphs.put(id, Graphs(List(), List()))
    id
  }

  override def getGraphs(id: TaskId): IO[Option[Graphs]] = IO {
    Option(graphs.get(id))
  }

  override def deleteGraphs(id: TaskId): IO[Either[String, Unit]] =
    IO(Option(graphs.remove(id)))
      .map {
        case Some(_) => Right(())
        case None => Left(s"There is no graphs for [$id]")
      }

  override def updateGraphs(id: TaskId, newGraphs: Graphs): IO[Either[String, Unit]] =
    IO(Option(graphs.get(id)))
      .map {
        case Some(_) =>
          graphs.put(id, newGraphs)
          Right(())
        case None =>
          Left(s"There is no graphs for [$id]")
      }

  override def getTasks(): IO[List[Task]] = IO(tasks.values()).map(_.asScala.toList)
}
