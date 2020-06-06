package com.yevhenii.cluster.planner.server

import java.util.concurrent.{Executor, ExecutorService, Executors, ForkJoinPool, ThreadPoolExecutor}

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.blaze._
import cats.implicits._
import com.softwaremill.tagging._
import com.typesafe.config.ConfigFactory
import com.yevhenii.cluster.planner.server.graphs.{MongoTaskRepository, TaskRoutes, TaskServiceImpl}
import com.yevhenii.cluster.planner.server.modeling.{PlanningRoutes, PlanningServiceImpl, QueueRoutes, QueueServiceImpl}
import com.yevhenii.cluster.planner.server.statistics.{StatisticsRoutes, StatisticsServiceImpl}
import com.yevhenii.cluster.planner.server.utils.Tags
import org.http4s.server.middleware.{CORS, GZip, Logger}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object Server extends IOApp {

  implicit val mongoEc: ExecutionContext @@ Tags.Mongo = ExecutionContext.fromExecutor(Executors.newCachedThreadPool()).taggedWith[Tags.Mongo]
  implicit val statsEc: ExecutionContext @@ Tags.Stats = ExecutionContext.fromExecutor(Executors.newWorkStealingPool()).taggedWith[Tags.Stats]

  val applicationConfig = ConfigFactory.load()

  val taskRepository = new MongoTaskRepository(applicationConfig)
  val taskService = new TaskServiceImpl(taskRepository)
  val queueService = new QueueServiceImpl(taskService)
  val planningService = new PlanningServiceImpl(taskService)
  val statisticsService = new StatisticsServiceImpl(taskService)

  val httpRoutes = Router[IO](
    "/" -> TaskRoutes.routes(taskService),
    "/" -> QueueRoutes.routes(queueService),
    "/" -> PlanningRoutes.routes(planningService),
    "/" -> StatisticsRoutes.routes(statisticsService)
  ).orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    val routesWithLog = Logger.httpApp(logHeaders = true, logBody = true)(httpRoutes)

    BlazeServerBuilder[IO]
      .bindHttp(applicationConfig.getInt("server.port"), "0.0.0.0")
      .withHttpApp(GZip(CORS(routesWithLog)))
      .withIdleTimeout(5.minutes)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
