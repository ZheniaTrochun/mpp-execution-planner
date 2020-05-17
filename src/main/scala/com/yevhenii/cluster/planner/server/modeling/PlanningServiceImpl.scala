package com.yevhenii.cluster.planner.server.modeling

import cats.Functor
import cats.instances.option._
import cats.effect.IO
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.graphs.{TaskRepository, TaskService}
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, NonOrientedGraphEntry, OrientedGraph, OrientedGraphEntry}


class PlanningServiceImpl(taskService: TaskService) extends PlanningService {

  override def buildGhantDiagramByConnectivity(id: TaskId, queueCreator: OrientedGraph => List[Node]): IO[Option[GhantDiagram]] = {
    val ioOptionFunctor = Functor[IO].compose(Functor[Option])

    val graphsIo = taskService.getGraphs(id)

    ioOptionFunctor.map(graphsIo) { graphs =>
      val taskGraph = OrientedGraph(OrientedGraphEntry(graphs.taskGraph))
      val systemGraph = NonOrientedGraph(NonOrientedGraphEntry(graphs.systemGraph))

      ExecutionPlanner.planExecutionByConnectivity(systemGraph, taskGraph, queueCreator)
    }
  }
}
