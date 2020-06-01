package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Graphs
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.graphs.TaskService
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, NonOrientedGraphEntry, OrientedGraph, OrientedGraphEntry}


class PlanningServiceImpl(taskService: TaskService) extends PlanningService with LazyLogging {

  type QueueCreator = OrientedGraph => List[Node]
  type Planner = (NonOrientedGraph, OrientedGraph, QueueCreator) => GhantDiagram

  override def buildGhantDiagramByConnectivity(id: TaskId, queueCreator: OrientedGraph => List[Node]): IO[Option[GhantDiagram]] = {
    IO.apply(logger.info("Creating Ghant diagram based on nodes connectivity"))
      .flatMap(_ => taskService.getGraphs(id))
      .map(_.map(graphs => planning(graphs, queueCreator, ExecutionPlanner.planExecutionByConnectivity)))
      .map(_.map { result =>
        logger.info(Show[GhantDiagram].show(result))
        result
      })
  }

  override def buildGhantDiagramByClosestNeighbor(id: TaskId, queueCreator: OrientedGraph => List[Node]): IO[Option[GhantDiagram]] = {
    IO.apply(logger.info("Creating Ghant diagram based on closest neighbor"))
      .flatMap(_ => taskService.getGraphs(id))
      .map(_.map(graphs => planning(graphs, queueCreator, ExecutionPlanner.planExecutionByClosestNeighbor)))
      .map(_.map { result =>
        logger.info(Show[GhantDiagram].show(result))
        result
      })
  }

  private def planning(graphs: Graphs, queueCreator: QueueCreator, planner: Planner): GhantDiagram = {
    val taskGraph = OrientedGraph(OrientedGraphEntry(graphs.taskGraph))
    val systemGraph = NonOrientedGraph(NonOrientedGraphEntry(graphs.systemGraph))

    planner(systemGraph, taskGraph, queueCreator)
  }
}
