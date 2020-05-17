package com.yevhenii.cluster.planner.server.graphs

import cats.effect.IO
import com.yevhenii.cluster.planner.server.dto.{Graphs, TaskInit}
import com.yevhenii.cluster.planner.server.model.GraphParameters
import io.circe.Json
import org.http4s.HttpRoutes
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object TaskRoutes extends Http4sDsl[IO] {

  private def errorBody(message: String): Json = Json.obj(
    ("message", Json.fromString(message))
  )

  def routes(taskService: TaskService): HttpRoutes[IO] = {

    HttpRoutes.of[IO] {
      case _ @ GET -> Root / "task" / "list" =>
        taskService.getTasks().flatMap(x => Ok(x))

      case req @ POST -> Root / "task" =>
        req.decode[TaskInit] { taskInit =>
          taskService.initTask(taskInit).flatMap { id =>
            Created(Json.obj(("id", Json.fromString(id))))
          }
        }

      case _ @ GET -> Root / "graphs" / id =>
        taskService.getGraphs(id).flatMap {
          case Some(graphs) => Ok(graphs)
          case None => NotFound()
        }

      case req @ PUT -> Root / "graphs" / id =>
        req.decode[Graphs] { graphs =>
          taskService.updateGraphs(id, graphs).flatMap {
            case Left(msg) => NotFound(errorBody(msg))
            case Right(_) => Ok()
          }
        }

      case req @ PUT -> Root / "graphs" / "random" / "task-taskGraph" / id =>
        req.decode[GraphParameters] { params =>
          taskService.generateTaskGraph(id, params).flatMap {
            case Some(graphs) => Ok(graphs)
            case None => NotFound()
          }
        }

      case _ @ DELETE -> Root / "task" / id =>
        taskService.deleteTask(id).flatMap {
          case Left(msg) => NotFound(errorBody(msg))
          case Right(_) => Ok()
        }
    }
  }
}
