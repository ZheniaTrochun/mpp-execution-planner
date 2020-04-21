package com.yevhenii.cluster.planner.server.modeling

import cats.effect.IO
import io.circe.Json
import org.http4s.HttpRoutes
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object QueueRoutes extends Http4sDsl[IO]  {
  private def errorBody(message: String): Json = Json.obj(
    ("message", Json.fromString(message))
  )

  def routes(queueService: QueueService): HttpRoutes[IO] = {

    HttpRoutes.of[IO] {
      case _ @ GET -> Root / "queue" / "critical-path" / id =>
        queueService.createQueueByCriticalPath(id)
          .flatMap {
            case Some(res) => Ok(res)
            case None => NotFound()
          }

      case _ @ GET -> Root / "queue" / "node-count-on-critical-path" / id =>
        queueService.createQueueByNodesCountOnCriticalPath(id)
          .flatMap {
            case Some(res) => Ok(res)
            case None => NotFound()
          }

      case _ @ GET -> Root / "queue" / "node-connectivity" / id =>
        queueService.createQueueByNodesConnectivity(id)
          .flatMap {
            case Some(res) => Ok(res)
            case None => NotFound()
          }
    }
  }
}
