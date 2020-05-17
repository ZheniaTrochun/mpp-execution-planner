package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Diagram
import com.yevhenii.cluster.planner.server.model.{Node, OrientedGraph}
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

    def buildDiagramByConnectivityAndCreateResponse(id: String, queueCreator: OrientedGraph => List[Node]): IO[Response[IO]] = {
      planningService.buildGhantDiagramByConnectivity(id, queueCreator)
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
        val queueCreator: OrientedGraph => List[Node] = graph => QueueCreator.createQueueBasedOnCriticalPath(graph).map(_._1)
        buildDiagramByConnectivityAndCreateResponse(id, queueCreator)

      case _ @ GET -> Root / "planning" / "node-count-on-critical-path" / "connectivity" / id =>
        val queueCreator: OrientedGraph => List[Node] = graph => QueueCreator.createQueueBasedOnNodesCountOnCriticalPath(graph).map(_._1)
        buildDiagramByConnectivityAndCreateResponse(id, queueCreator)

      case _ @ GET -> Root / "planning" / "node-connectivity" / "connectivity" / id =>
        val queueCreator: OrientedGraph => List[Node] = graph => QueueCreator.createQueueBasedOnNodesConnectivity(graph).map(_._1)
        buildDiagramByConnectivityAndCreateResponse(id, queueCreator)
    }
  }


}
