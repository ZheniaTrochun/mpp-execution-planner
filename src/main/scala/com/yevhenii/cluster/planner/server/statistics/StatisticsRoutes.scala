package com.yevhenii.cluster.planner.server.statistics

import cats.effect.{ContextShift, IO}
import com.softwaremill.tagging.@@
import com.yevhenii.cluster.planner.server.model.StatisticsParams
import com.yevhenii.cluster.planner.server.utils.Tags
import io.circe.Json
import org.http4s.HttpRoutes
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

object StatisticsRoutes extends Http4sDsl[IO] {

  private def errorBody(message: String): Json = Json.obj(
    ("message", Json.fromString(message))
  )

  def routes(statisticsService: StatisticsService)(implicit cs: ContextShift[IO], ec: ExecutionContext @@ Tags.Stats): HttpRoutes[IO] = {

    HttpRoutes.of[IO] {
      case req @ POST -> Root / "stats" / id =>
        req.decode[StatisticsParams] { params =>
          statisticsService.collectStatistics(id, params)
            .flatMap {
              case Some(resp) => Ok(resp)
              case None => NotFound()
            }
        }
    }
  }
}
