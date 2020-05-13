package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import com.yevhenii.cluster.planner.server.model.Node
import com.yevhenii.cluster.planner.server.modeling.ComputingGhantDiagram.Computing

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ComputingGhantDiagram(processingNodes: List[Node]) {
  val processingNodesMap = processingNodes.map(node => node.id -> node).toMap

  val processors: mutable.Map[String, ListBuffer[Option[Computing]]] =
    mutable.Map(processingNodes.map(_.id -> ListBuffer.empty[Option[Computing]]): _*)

  val isReserved: mutable.Map[String, Boolean] = mutable.Map(processingNodes.map(_.id -> false): _*)

  def isProcessorReserved(id: String): Boolean = isReserved(id)

  def schedule(id: String, task: Node, start: Int): Either[String, Unit] = {
    val queueOfTasks = processors(id)

    if (isFree(id, start)) {
      for (_ <- queueOfTasks.size until start) {
        queueOfTasks.append(None)
      }

      val durationOfTask = Math.ceil(task.weight.toDouble / processingNodesMap(id).weight).toInt

      for (_ <- start until (start + durationOfTask)) {
        queueOfTasks.append(Some(Computing(task)))
      }

      Right(())
    } else {
      Left(s"Processor is busy at tact $start computing [${queueOfTasks(start)}]")
    }
  }

  def isFree(id: String, start: Int): Boolean = {
    val queueOfTasks = processors(id)
    queueOfTasks.size <= start
  }

  def freeProcessorIds(start: Int): List[String] = {
    processors.keys.filter(isFree(_, start)).toList
  }

  def findRecentlyFinished(tact: Int): List[Node] = {
    processors.values
      .filter(_.size == tact)
      .flatMap(_.last)
      .map(_.node)
      .toList
  }

  override def equals(obj: Any): Boolean = {
    if (obj != null && obj.isInstanceOf[ComputingGhantDiagram]) {
      val other = obj.asInstanceOf[ComputingGhantDiagram]
      this.processors == other.processors
    } else {
      false
    }
  }

  override def toString: String = Show[ComputingGhantDiagram].show(this)
}

object ComputingGhantDiagram {
  case class Computing(node: Node)

  implicit val show: Show[ComputingGhantDiagram] = diagram => {
    diagram.processors.toList
      .sortBy(_._1)
      .map { case (id, queue) =>
        val queueStr = queue.map(opt => opt.map(task => s"${task.node.id}").getOrElse("...")).mkString("|", "|", "|")
        s"$id: $queueStr"
      }
      .mkString("\n")
  }
}
