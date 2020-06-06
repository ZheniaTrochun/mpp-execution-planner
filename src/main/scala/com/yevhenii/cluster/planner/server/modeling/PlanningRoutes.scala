package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Diagram
import com.yevhenii.cluster.planner.server.modeling.Queues.QueueCreator
import io.circe.Json
import org.http4s.{HttpRoutes, Response}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object PlanningRoutes extends Http4sDsl[IO] with LazyLogging {

  private def errorBody(message: String): Json = Json.obj(
    ("message", Json.fromString(message))
  )

  def routes(planningService: PlanningService): HttpRoutes[IO] = {

    def buildDiagramAndCreateResponse(
      id: String,
      queueCreator: QueueCreator)(
      planning: (String, QueueCreator) => IO[Option[GhantDiagram]]
    ): IO[Response[IO]] = {

      planning(id, queueCreator)
        .flatMap {
          case Some(res) =>
            val x = Diagram.from(res)
            logger.info(Show[Diagram].show(x))
            Ok(x)
          case None => NotFound()
        }
    }

    HttpRoutes.of[IO] {
      case _ @ GET -> Root / "planning" / "critical-path" / "connectivity" / id =>
        buildDiagramAndCreateResponse(id, Queues.CriticalPathBased)(planningService.buildGhantDiagramByConnectivity)

      case _ @ GET -> Root / "planning" / "node-count-on-critical-path" / "connectivity" / id =>
        buildDiagramAndCreateResponse(id, Queues.NodesOnCriticalPathBased)(planningService.buildGhantDiagramByConnectivity)

      case _ @ GET -> Root / "planning" / "node-connectivity" / "connectivity" / id =>
        buildDiagramAndCreateResponse(id, Queues.ConnectivityBased)(planningService.buildGhantDiagramByConnectivity)

      case _ @ GET -> Root / "planning" / "critical-path" / "closest-neighbor" / id =>
        buildDiagramAndCreateResponse(id, Queues.CriticalPathBased)(planningService.buildGhantDiagramByClosestNeighbor)

      case _ @ GET -> Root / "planning" / "node-count-on-critical-path" / "closest-neighbor" / id =>
        buildDiagramAndCreateResponse(id, Queues.NodesOnCriticalPathBased)(planningService.buildGhantDiagramByClosestNeighbor)

      case _ @ GET -> Root / "planning" / "node-connectivity" / "closest-neighbor" / id =>
        buildDiagramAndCreateResponse(id, Queues.ConnectivityBased)(planningService.buildGhantDiagramByClosestNeighbor)
    }
  }
}
