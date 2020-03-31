package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.models.{Graphs, TaskInit}
import io.circe.Json
import org.http4s.HttpRoutes
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object TaskRoutes extends Http4sDsl[IO] {

  private def errorBody(message: String): Json = Json.obj(
    ("message", Json.fromString(message))
  )

  def routes(tasksRepo: TaskRepository): HttpRoutes[IO] = {

    HttpRoutes.of[IO] {
      case _ @ GET -> Root / "task" / "list" =>
        tasksRepo.getTasks().flatMap(x => Ok(x))

      case req @ POST -> Root / "task" =>
        req.decode[TaskInit] { taskInit =>
          tasksRepo.initTask(taskInit).flatMap { id =>
            Created(Json.obj(("id", Json.fromString(id))))
          }
        }

      case _ @ GET -> Root / "graphs" / id =>
        tasksRepo.getGraphs(id).flatMap {
          case Some(graphs) => Ok(graphs)
          case None => NotFound()
        }

      case req @ PUT -> Root / "graphs" / id =>
        req.decode[Graphs] { graphs =>
          tasksRepo.updateGraphs(id, graphs).flatMap {
            case Left(msg) => NotFound(errorBody(msg))
            case Right(_) => Ok()
          }
        }

      case _ @ DELETE -> Root / "graphs" / id =>
        tasksRepo.deleteGraphs(id).flatMap {
          case Left(msg) => NotFound(errorBody(msg))
          case Right(_) => Ok()
        }
    }
  }
}
