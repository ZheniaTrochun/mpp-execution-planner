import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.yevhenii"
ThisBuild / name := "cluster.planner.server"

assemblyJarName in assembly := "cluster-planner-server.jar"
mainClass in assembly := Some("com.yevhenii.cluster.planner.server.Server")

lazy val root = (project in file("."))
  .settings(
    name := "ClusterPlanner",
    libraryDependencies ++= allHttp4s ++ Seq(
      catsIO,
      mongo,
      typesafeConfig,
      logging,
      scalaTest % Test
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )

val stage = taskKey[Unit]("Stage task")

val Stage = config("stage")

stage := {
  assembly.value
}