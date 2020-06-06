package com.yevhenii.cluster.planner.server.statistics

import cats.effect.{ContextShift, IO}
import com.softwaremill.tagging.@@
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.graphs.TaskService
import com.yevhenii.cluster.planner.server.model.{NonOrientedGraph, NonOrientedGraphEntry, StatisticsParams, Stats}
import com.yevhenii.cluster.planner.server.utils.Tags

import scala.concurrent.ExecutionContext

trait StatisticsService {

  def collectStatistics(taskId: String, request: StatisticsParams): IO[Option[List[Stats]]]
}

class StatisticsServiceImpl(taskService: TaskService)(implicit cs: ContextShift[IO], ec: ExecutionContext @@ Tags.Stats) extends StatisticsService with LazyLogging {

  override def collectStatistics(taskId: String, parameters: StatisticsParams): IO[Option[List[Stats]]] =
    IO.apply(logger.info(s"Collecting statistics for $taskId"))
      .flatMap(_ => taskService.getGraphs(taskId))
      .map(_.map(x => NonOrientedGraph(NonOrientedGraphEntry(x.systemGraph))))
      .flatMap {
        case None =>
          IO.pure(None)

        case Some(systemGraph) =>
          val statsIo = IO.apply(StatisticsCalculator.calculateStatistics(systemGraph, parameters))
          IO.fromFuture(statsIo)
            .map(Some.apply)
      }
}
