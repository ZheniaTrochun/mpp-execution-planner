package com.yevhenii.cluster.planner.server

import cats.effect.{ExitCode, IO, IOApp}
import com.yevhenii.cluster.planner.server.graphs.TaskRoutes
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.blaze._
import cats.implicits._
import com.typesafe.config.ConfigFactory
import com.yevhenii.cluster.planner.server.graphs.{MongoTaskRepository, TaskRoutes}
import org.http4s.server.middleware.{CORS, GZip, Logger}

import scala.concurrent.ExecutionContext

object Server extends IOApp {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val applicationConfig = ConfigFactory.load()
  val taskRepository = new MongoTaskRepository(applicationConfig)

  val httpRoutes = Router[IO](
    "/" -> TaskRoutes.routes(taskRepository)
  ).orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    val routesWithLog = Logger.httpApp(logHeaders = true, logBody = true)(httpRoutes)

    BlazeServerBuilder[IO]
      .bindHttp(9090, "0.0.0.0")
      .withHttpApp(GZip(CORS(routesWithLog)))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
