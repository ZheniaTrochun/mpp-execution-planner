package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Graphs
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.graphs.TaskService
import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.Planners._
import com.yevhenii.cluster.planner.server.modeling.Queues.QueueCreator

trait PlanningService {

  def buildGhantDiagramByConnectivity(id: TaskId, queueCreator: QueueCreator): IO[Option[GhantDiagram]]
  def buildGhantDiagramByClosestNeighbor(id: TaskId, queueCreator: QueueCreator): IO[Option[GhantDiagram]]
}

class PlanningServiceImpl(taskService: TaskService) extends PlanningService with LazyLogging {

  override def buildGhantDiagramByConnectivity(id: TaskId, queueCreator: QueueCreator): IO[Option[GhantDiagram]] = {
    IO.apply(logger.info("Creating Ghant diagram based on nodes connectivity"))
      .flatMap(_ => taskService.getGraphs(id))
      .map(_.map(graphs => planning(graphs, queueCreator, ConnectivityPlanner)))
      .map(_.map { result =>
        logger.info(Show[GhantDiagram].show(result))
        result
      })
  }

  override def buildGhantDiagramByClosestNeighbor(id: TaskId, queueCreator: QueueCreator): IO[Option[GhantDiagram]] = {
    IO.apply(logger.info("Creating Ghant diagram based on closest neighbor"))
      .flatMap(_ => taskService.getGraphs(id))
      .map(_.map(graphs => planning(graphs, queueCreator, CloseNeighborPlanner)))
      .map(_.map { result =>
        logger.info(Show[GhantDiagram].show(result))
        result
      })
  }

  private def planning(graphs: Graphs, queueCreator: QueueCreator, planner: ExecutionPlanner): GhantDiagram = {
    val taskGraph = OrientedGraph(OrientedGraphEntry(graphs.taskGraph))
    val systemGraph = NonOrientedGraph(NonOrientedGraphEntry(graphs.systemGraph))

    planner(systemGraph, taskGraph, queueCreator)
  }
}
