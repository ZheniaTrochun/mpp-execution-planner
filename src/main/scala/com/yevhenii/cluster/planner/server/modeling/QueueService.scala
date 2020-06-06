package com.yevhenii.cluster.planner.server.modeling

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.dto.Data
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.graphs.TaskService
import com.yevhenii.cluster.planner.server.model.{Node, OrientedGraph, OrientedGraphEntry}

trait QueueService {

  type QueueWithPaths = List[(Data, List[Data])]

  def createQueueByCriticalPath(id: TaskId): IO[Option[QueueWithPaths]]
  def createQueueByNodesCountOnCriticalPath(id: TaskId): IO[Option[QueueWithPaths]]

  type QueueWithConnAndLength = List[(Data, (Int, Int))]

  def createQueueByNodesConnectivity(id: TaskId): IO[Option[QueueWithConnAndLength]]
}

class QueueServiceImpl(taskService: TaskService) extends QueueService with LazyLogging {

  override def createQueueByCriticalPath(id: TaskId): IO[Option[QueueWithPaths]] =
    IO(logger.info(s"Building queue based on critical path for taskId [$id]"))
      .flatMap(_ =>
        extractTaskAndCreateQueue(id)(taskGraph => Queues.CriticalPathBased.applyExtended(taskGraph), queueWithPathToDto)
      )

  override def createQueueByNodesCountOnCriticalPath(id: TaskId): IO[Option[QueueWithPaths]] =
    IO(logger.info(s"Building queue based on critical path by node count for taskId [$id]"))
      .flatMap(_ =>
        extractTaskAndCreateQueue(id)(taskGraph => Queues.NodesOnCriticalPathBased.applyExtended(taskGraph), queueWithPathToDto)
      )

  override def createQueueByNodesConnectivity(id: TaskId): IO[Option[QueueWithConnAndLength]] =
    IO(logger.info(s"Building queue based on nodes connectivity for taskId [$id]"))
      .flatMap(_ =>
        extractTaskAndCreateQueue(id)(taskGraph => Queues.ConnectivityBased.applyExtended(taskGraph), queueWithConnAndLengthToDto)
      )

  private def extractTaskAndCreateQueue[A, B](id: TaskId)(createQueue: OrientedGraph => A, andThen: A => B): IO[Option[B]] =
    taskService.getGraphs(id)
      .map(_.map { graphs =>
        val taskGraph = OrientedGraph(OrientedGraphEntry(graphs.taskGraph))
        createQueue(taskGraph)
      })
      .map(_.map(andThen))

  private def queueWithPathToDto(queue: List[(Node, List[Node])]): QueueWithPaths =
    queue.map { case (node, path) => (Data.from(node), path.map(Data.from)) }

  private def queueWithConnAndLengthToDto(queue: List[(Node, (Int, Int))]): QueueWithConnAndLength =
    queue.map { case (node, (connectivity, pathLength)) => (Data.from(node), (connectivity, pathLength)) }
}
