package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedGraph, OrientedEdge, OrientedGraph}
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._

import scala.annotation.tailrec

object ExecutionPlanner extends LazyLogging {

  val MaxIterationNumber = 1000

  case class TaskContext(taskQueue: List[Node], nodesQueue: List[Node], taskGraph: OrientedGraph, systemGraph: NonOrientedGraph)
  case class TactContext(tact: Int, finishedTasks: List[Node], scheduledTasks: List[Node])

  trait Planner extends ((NonOrientedGraph, OrientedGraph, OrientedGraph => List[Node]) => GhantDiagram) {

    override def apply(
      systemGraph: NonOrientedGraph,
      taskGraph: OrientedGraph,
      queueCreator: OrientedGraph => List[Node]
    ): GhantDiagram = {

      val queueOfNodes = orderOfComputingNodes(systemGraph)
      val queueOfTasks = queueCreator(taskGraph)

      val (diagram, scheduled) = initComputing(queueOfNodes, queueOfTasks, taskGraph, systemGraph)

      loopOfPlanning(
        TaskContext(queueOfTasks, queueOfNodes, taskGraph, systemGraph),
        TactContext(1, List.empty, scheduled),
        diagram
      )
    }

    @tailrec
    private def loopOfPlanning(
      taskContext: TaskContext,
      tactContext: TactContext,
      diagram: GhantDiagram
    ): GhantDiagram = {

      val TactContext(tact, finishedTasks, scheduledTasks) = tactContext
      val TaskContext(taskQueue, _, taskGraph, _) = taskContext

      if (finishedTasks.size == taskQueue.size || tact == MaxIterationNumber) {
        diagram
      } else {

        val recentlyFinished = diagram.findRecentlyFinished(tact)

        val newComputed = recentlyFinished ::: finishedTasks
        val readyNodes = findTasksReadyToBeComputed(taskGraph, taskQueue, scheduledTasks, newComputed)

        val newScheduled = schedule(tact, readyNodes, taskContext, diagram)

        loopOfPlanning(taskContext, TactContext(tact + 1, newComputed, newScheduled ::: scheduledTasks), diagram)
      }
    }

    protected def schedule(tact: Int, readyNodes: List[Node], taskContext: TaskContext, diagram: GhantDiagram): List[Node]
  }

  object ConnectivityPlanner extends Planner {

    override protected def schedule(tact: Int, readyNodes: List[Node], taskContext: TaskContext, diagram: GhantDiagram): List[Node] = {
      val freeProcessors = taskContext.nodesQueue.filter(n => diagram.isFree(n.id, tact))

      readyNodes.zip(freeProcessors)
        .map { case (node, processor) => scheduleToProcessor(diagram, tact, taskContext.taskGraph, node, processor) }
    }

    private def scheduleToProcessor(diagram: GhantDiagram, tact: Int, taskGraph: OrientedGraph, node: Node, processor: Node): Node = {
      val parentData = taskGraph.edges.filter(_.target == node.id)
      val whenEverythingIsTransferred = parentData
        .flatMap { edge =>
          val whereWasComputed = diagram.whereWasComputed(edge.source)
          whereWasComputed.map(n => edge -> n)
        }
        .foldLeft(tact) { (startAt, whereWasComputedPair) =>
          val (edge, whereWasComputed) = whereWasComputedPair
          diagram.transferTo(whereWasComputed, processor.id, edge, startAt)
        }

      diagram.schedule(processor.id, node, whenEverythingIsTransferred)
      node
    }
  }

  object CloseNeighborPlanner extends Planner {

    override protected def schedule(tact: Int, readyNodes: List[Node], taskContext: TaskContext, diagram: GhantDiagram): List[Node] = {

      val numberOfFreeProcessors = diagram.freeProcessorIds(tact).size

      readyNodes.take(numberOfFreeProcessors).map { node =>
        val parentData: List[(OrientedEdge, (String, Int))] = taskContext.taskGraph.edges.filter(_.target == node.id)
          .flatMap(edge => diagram.whereAndWhenWasComputed(edge.source).map(x => edge -> x))

        val processor = chooseBestProcessor(tact, taskContext, diagram, parentData)

        val whenEverythingIsTransferred = if (parentData.isEmpty) {
          0
        } else {
          parentData
            .map { case (edge, (whereComputed, whenComputed)) =>
              diagram.transferTo(whereComputed, processor, edge, whenComputed + 1) // todo: maybe should be "whenComputed"
            }
            .max
        }

        diagram.schedule(processor, node, whenEverythingIsTransferred.max(tact))
        node
      }
    }

//    // todo: I'm not really sure if it works correctly
//    private def chooseBestProcessor(tact: Int, taskContext: TaskContext, diagram: GhantDiagram, parentData: List[(OrientedEdge, (String, Int))]): String = {
//      diagram.freeProcessorIds(tact)
//        .map { processor =>
//          val timeToTransfer = parentData
//            .map { case (edge, (whereComputed, _)) =>
//              (edge, taskContext.systemGraph.findShortestPath(whereComputed, processor, edge.weight))
//            }
//            .map { case (edge, path) =>
//              path.foldLeft(0)((acc, currEdge) => acc + math.ceil(edge.weight.toDouble / currEdge.weight).toInt)
//            }
//            .sum
//
//          val approximateStartTime =
//            if (parentData.nonEmpty) {
//              parentData
//                .map { case (edge, (whereComputed, whenComputed)) =>
//                  diagram.approximateStartTime(whereComputed, processor, edge, whenComputed)
//                }
//                .max
//            } else {
//              tact
//            }
//
//          (timeToTransfer, processor, approximateStartTime)
//        }
//        .min(Ordering.Tuple3(Ordering[Int], Ordering.by[String, Int](x => taskContext.nodesQueue.map(_.id).indexOf(x)).reverse, Ordering[Int]))
//        ._2
//    }

//    // todo: I'm not really sure if it works correctly
//    private def chooseBestProcessor(tact: Int, taskContext: TaskContext, diagram: GhantDiagram, parentData: List[(OrientedEdge, (String, Int))]): String = {
//      diagram.freeProcessorIds(tact)
//        .map { processor =>
//          val timeToTransfer = parentData
//            .map { case (edge, (whereComputed, _)) =>
//              (edge, taskContext.systemGraph.findShortestPath(whereComputed, processor, edge.weight))
//            }
//            .map { case (edge, path) =>
//              path.foldLeft(0)((acc, currEdge) => acc + math.ceil(edge.weight.toDouble / currEdge.weight).toInt)
//            }
//            .sum
//
//          val approximateStartTime =
//            if (parentData.nonEmpty) {
//              parentData
//                .map { case (edge, (whereComputed, whenComputed)) =>
//                  diagram.approximateStartTime(whereComputed, processor, edge, whenComputed)
//                }
//                .max
//            } else {
//              tact
//            }
//
//          (timeToTransfer, approximateStartTime, processor)
//        }
//        .min(Ordering.Tuple3(Ordering[Int], Ordering[Int], Ordering.by[String, Int](x => taskContext.nodesQueue.map(_.id).indexOf(x)).reverse))
//        ._3
//    }

    private def chooseBestProcessor(tact: Int, taskContext: TaskContext, diagram: GhantDiagram, parentData: List[(OrientedEdge, (String, Int))]): String = {
      diagram.freeProcessorIds(tact)
        .map { processor =>
          val timeToTransfer = parentData
            .map { case (edge, (whereComputed, _)) =>
              (edge, taskContext.systemGraph.findShortestPath(whereComputed, processor, edge.weight))
            }
            .map { case (edge, path) =>
              path.foldLeft(0)((acc, currEdge) => acc + math.ceil(edge.weight.toDouble / currEdge.weight).toInt)
            }
            .sum

          (processor, timeToTransfer)
        }
        .map(_.swap) //{ case (processor, path) => (path, processor) }
        .min(Ordering.Tuple2(Ordering[Int], Ordering.by[String, Int](x => taskContext.nodesQueue.map(_.id).indexOf(x)).reverse))
        ._2
    }
  }

  private def orderOfComputingNodes(nonOrientedGraph: NonOrientedGraph): List[Node] =
    nonOrientedGraph.nodes.sortBy(nonOrientedGraph.connectivityOfNode)(Ordering.Int.reverse)

  private def isTaskReadyToBeComputed(task: Node, taskGraph: OrientedGraph, computedNodes: List[Node]): Boolean = {
    val computedNodeIds = computedNodes.map(_.id)
    val parents = taskGraph.edges.filter(edge => edge.target == task.id).map(_.source)

    parents.forall(computedNodeIds.contains)
  }

  private def findTasksReadyToBeComputed(taskGraph: OrientedGraph, taskQueue: List[Node], scheduled: List[Node], computedNodes: List[Node]): List[Node] = {
    taskQueue
      .view
      .filterNot(t => computedNodes.exists(_.id == t.id))
      .filterNot(t => scheduled.exists(_.id == t.id))
      .filter(isTaskReadyToBeComputed(_, taskGraph, computedNodes))
      .toList
  }

  private def initComputing(
    computingNodesQueue: List[Node],
    queueOfTasks: List[Node],
    taskGraph: OrientedGraph,
    systemGraph: NonOrientedGraph
  ): (GhantDiagram, List[Node]) = {

    val diagram = new GhantDiagram(systemGraph)

    val scheduled = queueOfTasks
      .filter(isTaskReadyToBeComputed(_, taskGraph, List.empty))
      .take(computingNodesQueue.size)
      .zip(computingNodesQueue)
      .map { case (work, node) =>
        diagram.schedule(node.id, work, 0)
        work
      }

    logger.info(s"initially scheduled: ${scheduled.map(_.id).mkString(", ")}")

    (diagram, scheduled)
  }

  implicit val nodeQueueShow: Show[List[Node]] = queue => queue.map(_.id).mkString(" -> ")
}
