package com.yevhenii.cluster.planner.server.statistics

import cats.Show
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.graphs.GraphRandomizer
import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.Planners.ExecutionPlanner
import com.yevhenii.cluster.planner.server.modeling.Queues.QueueCreator
import com.yevhenii.cluster.planner.server.modeling.{Planners, Queues}

import scala.concurrent.{ExecutionContext, Future}

object StatisticsCalculator extends LazyLogging {

//  val NumberOfSameGraphs = 5
  val NumberOfSameGraphs = 1

  def calculateStatistics(systemGraph: NonOrientedGraph, params: StatisticsParams)(implicit ec: ExecutionContext): Future[List[Stats]] = {
    val start = System.currentTimeMillis()

    val graphsFuture = generateGraphs(params, systemGraph.nodes.size)

    graphsFuture.flatMap { graphs =>
      val endOfGeneration = System.currentTimeMillis()
      logger.info(s"Finished generation of ${graphs.size} graphs in ${endOfGeneration - start}")

      Future.traverse(graphs) { graph =>
        Future {
          planAllExecutions(systemGraph, graph._2, graph._1)
        }
      }.map(_.flatten)
    }
  }

  private def generateGraphs(params: StatisticsParams, taskGraphSize: Int)(implicit ec: ExecutionContext): Future[List[(Double, List[OrientedGraph])]] = {
    val StatisticsParams(maxSizeMultiplier, connectivityStart, connectivityLimit, connectivityStep) = params

    val generationParams = for {
      size <- taskGraphSize to (taskGraphSize * maxSizeMultiplier) by taskGraphSize
      correlation <- connectivityStart to connectivityLimit by connectivityStep
    } yield GraphParameters(numberOfNodes = size, correlation = correlation)

    Future.traverse(generationParams.toList) { graphParams =>
      Future {
        for (_ <- 1 to NumberOfSameGraphs) yield GraphRandomizer.randomOrientedGraph(graphParams)
      }.map(x => (graphParams.correlation, x.toList))
    }
  }

  private def planAllExecutions(systemGraph: NonOrientedGraph, taskGraphs: List[OrientedGraph], correlation: Double): List[Stats] = {
//    val executions = for {
//      queue <- Queues.Algorithms
//      planner <- Planners.Algorithms
//      taskGraph <- taskGraphs
//      diagram = planner.apply(systemGraph, taskGraph, queue)
//    } yield Stats(
//      Show[QueueCreator].show(queue),
//      Show[ExecutionPlanner].show(planner),
//      taskGraph.nodes.size,
//      diagram.finishedIn(),
//      diagram.speedup(),
//      diagram.efficiency()
//    )

    if (taskGraphs.isEmpty) {
      List.empty
    } else {
      for {
        queue <- Queues.Algorithms
        planner <- Planners.Algorithms
      } yield averageStats(systemGraph, taskGraphs, correlation, queue, planner)
    }
  }

  private def averageStats(systemGraph: NonOrientedGraph, taskGraphs: List[OrientedGraph], correlation: Double, queue: QueueCreator, planner: ExecutionPlanner): Stats = {
    val diagrams = taskGraphs.map(task => planner.apply(systemGraph, task, queue))

    val time = diagrams.map(_.finishedIn()).sum / diagrams.size
    val speedup = diagrams.map(_.speedup()).sum / diagrams.size
    val efficiency = diagrams.map(_.efficiency()).sum / diagrams.size
    val algorithmEfficiency = diagrams.map(_.algorithmEfficiency()).sum / diagrams.size

    Stats(
      Show[QueueCreator].show(queue),
      Show[ExecutionPlanner].show(planner),
      taskGraphs.head.nodes.size,
      correlation,
      time,
      speedup,
      efficiency,
      algorithmEfficiency
    )
  }
}
