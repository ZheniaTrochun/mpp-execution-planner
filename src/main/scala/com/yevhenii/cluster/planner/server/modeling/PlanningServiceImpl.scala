package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.graphs.TaskService
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, NonOrientedGraphEntry, OrientedGraph, OrientedGraphEntry}


class PlanningServiceImpl(taskService: TaskService) extends PlanningService with LazyLogging {

  override def buildGhantDiagramByConnectivity(id: TaskId, queueCreator: OrientedGraph => List[Node]): IO[Option[GhantDiagram]] = {
    IO.apply(logger.info("Creating Ghant diagram based on nodes connectivity"))
      .flatMap(_ => taskService.getGraphs(id))
      .map(_.map { graphs =>
        val taskGraph = OrientedGraph(OrientedGraphEntry(graphs.taskGraph))
        val systemGraph = NonOrientedGraph(NonOrientedGraphEntry(graphs.systemGraph))

        val result = ExecutionPlanner.planExecutionByConnectivity(systemGraph, taskGraph, queueCreator)

        logger.info(Show[GhantDiagram].show(result))
        result
      })
  }
}
