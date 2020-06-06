package com.yevhenii.cluster.planner.server.statistics

import cats.Show
import com.yevhenii.cluster.planner.server.graphs.GraphRandomizer
import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.Planners.ExecutionPlanner
import com.yevhenii.cluster.planner.server.modeling.Queues.QueueCreator
import com.yevhenii.cluster.planner.server.modeling.{Planners, Queues}

import scala.concurrent.{ExecutionContext, Future}

object StatisticsCalculator {

//  val NumberOfSameGraphs = 5
  val NumberOfSameGraphs = 1

  def calculateStatistics(systemGraph: NonOrientedGraph, params: StatisticsParams)(implicit ec: ExecutionContext): Future[List[Stats]] = {
    val graphsFuture = generateGraphs(params, systemGraph.nodes.size)

    graphsFuture.flatMap { graphs =>
      Future.traverse(graphs) { graph =>
        Future {
          planAllExecutions(systemGraph, graph)
        }
      }.map(_.flatten)
    }
  }

  private def generateGraphs(params: StatisticsParams, taskGraphSize: Int)(implicit ec: ExecutionContext): Future[List[List[OrientedGraph]]] = {
    val StatisticsParams(maxSizeMultiplier, connectivityStart, connectivityLimit, connectivityStep) = params

    val generationParams = for {
      size <- taskGraphSize to (taskGraphSize * maxSizeMultiplier) by taskGraphSize
      connectivity <- connectivityStart to connectivityLimit by connectivityStep
    } yield GraphParameters(numberOfNodes = size, correlation = connectivity)

    Future.traverse(generationParams.toList) { graphParams =>
      Future {
        for (_ <- 1 to NumberOfSameGraphs) yield GraphRandomizer.randomOrientedGraph(graphParams)
      }.map(_.toList)
    }
  }

  private def planAllExecutions(systemGraph: NonOrientedGraph, taskGraphs: List[OrientedGraph]): List[Stats] = {
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
      } yield averageStats(systemGraph, taskGraphs, queue, planner)
    }
  }

  private def averageStats(systemGraph: NonOrientedGraph, taskGraphs: List[OrientedGraph], queue: QueueCreator, planner: ExecutionPlanner): Stats = {
    val diagrams = taskGraphs.map(task => planner.apply(systemGraph, task, queue))

    val time = diagrams.map(_.finishedIn()).sum / diagrams.size
    val speedup = diagrams.map(_.speedup()).sum / diagrams.size
    val efficiency = diagrams.map(_.efficiency()).sum / diagrams.size

    Stats(
      Show[QueueCreator].show(queue),
      Show[ExecutionPlanner].show(planner),
      taskGraphs.head.nodes.size,
      time,
      speedup,
      efficiency
    )
  }
}
