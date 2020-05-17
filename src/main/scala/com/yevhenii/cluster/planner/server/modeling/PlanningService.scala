package com.yevhenii.cluster.planner.server.modeling

import cats.effect.IO
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.model.{Node, OrientedGraph}

trait PlanningService {

  def buildGhantDiagramByConnectivity(id: TaskId, queueCreator: OrientedGraph => List[Node]): IO[Option[GhantDiagram]]
}
