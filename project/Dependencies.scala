import sbt._

object Dependencies {
  object Versions {
    val Http4s = "0.21.2"
    val Circe = "0.13.0"
    val Logback = "1.2.3"
    val CatsIO = "2.1.2"
    val MongoDriver = "2.6.0"
    val Scalatest = "3.0.8"
    val Config = "1.4.0"
    val Logging = "3.9.2"
  }
  
  lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.Scalatest
  lazy val catsIO = "org.typelevel" %% "cats-effect" % Versions.CatsIO
  lazy val mongo = "org.mongodb.scala" %% "mongo-scala-driver" % Versions.MongoDriver
  lazy val typesafeConfig = "com.typesafe" % "config" % Versions.Config
  lazy val logging = "com.typesafe.scala-logging" %% "scala-logging" % Versions.Logging

  lazy val allHttp4s = Seq(
    "org.http4s"      %% "http4s-blaze-server" % Versions.Http4s,
    "org.http4s"      %% "http4s-blaze-client" % Versions.Http4s,
    "org.http4s"      %% "http4s-circe"        % Versions.Http4s,
    "org.http4s"      %% "http4s-dsl"          % Versions.Http4s,
    "io.circe"        %% "circe-generic"       % Versions.Circe,
    "io.circe"        %% "circe-core"          % Versions.Circe,
    "ch.qos.logback"  %  "logback-classic"     % Versions.Logback
  )
}
