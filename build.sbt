import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.yevhenii"
//ThisBuild / organizationName := "cluster.planner.server"
ThisBuild / name := "cluster.planner.server"

lazy val root = (project in file("."))
  .settings(
    name := "ClusterPlanner",
    libraryDependencies ++= allHttp4s ++ Seq(
      catsIO,
      mongo,
      typesafeConfig,
      scalaTest % Test
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
