package com.yevhenii.cluster.planner.server.modeling

import cats.effect.IO
import com.yevhenii.cluster.planner.server.dto.Data
import com.yevhenii.cluster.planner.server.dto.Task.TaskId

trait QueueService {

  type QueueWithPaths = List[(Data, List[Data])]

  def createQueueByCriticalPath(id: TaskId): IO[Option[QueueWithPaths]]
  def createQueueByNodesCountOnCriticalPath(id: TaskId): IO[Option[QueueWithPaths]]

  type QueueWithConnAndLength = List[(Data, (Int, Int))]

  def createQueueByNodesConnectivity(id: TaskId): IO[Option[QueueWithConnAndLength]]
}
