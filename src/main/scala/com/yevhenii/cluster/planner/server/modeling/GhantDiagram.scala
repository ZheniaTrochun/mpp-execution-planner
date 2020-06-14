package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._

class GhantDiagram(systemGraph: NonOrientedGraph, taskGraph: OrientedGraph) {

  val computationDiagram: ComputingGhantDiagram = new ComputingGhantDiagram(systemGraph.nodes)
  val transferDiagram: TransferGhantDiagram = new TransferGhantDiagram(systemGraph)

  def isProcessorReserved(id: String): Boolean = computationDiagram.isReserved(id)

  def schedule(id: String, task: Node, start: Int): Either[String, Unit] = computationDiagram.schedule(id, task, start)

  def isFree(id: String, start: Int): Boolean = computationDiagram.isFree(id, start)

  def whereWasComputed(id: String): Option[String] = computationDiagram.whereWasComputed(id)

  def whereAndWhenWasComputed(id: String): Option[(String, Int)] = computationDiagram.whereAndWhenWasComputed(id)

  def freeProcessorIds(start: Int): List[String] = computationDiagram.freeProcessorIds(start)

  def findRecentlyFinished(tact: Int): List[Node] = computationDiagram.findRecentlyFinished(tact)

  def transferTo(from: String, to: String, data: OrientedEdge, atLeastStartingFrom: Int): Int =
    transferDiagram.transferTo(from, to, data, atLeastStartingFrom)

  def scheduleDirectTransfer(from: String, to: String, data: OrientedEdge, atLeastStartingFrom: Int): Option[Int] =
    transferDiagram.scheduleDirectTransfer(from, to, data, atLeastStartingFrom)

  def approximateStartTime(from: String, to: String, data: OrientedEdge, atLeastStartingFrom: Int): Int =
    transferDiagram.approximateStartTime(from, to, data, atLeastStartingFrom)

  def finishedIn(): Int = {
    computationDiagram.processors.values.map(_.size).max
  }

  def speedup(): Double = {
    val time = finishedIn()

    if (time == 0) 0
    else taskGraph.sumOfWeights.toDouble / time
  }

  def efficiency(): Double = {
    if (taskGraph.nodes.isEmpty) 0
    else speedup() / systemGraph.nodes.size
  }

  def algorithmEfficiency(): Double = {
    val time = finishedIn()

    if (time == 0) 0
    else taskGraph.criticalPath.map(_.weight).sum.toDouble / time
  }

  override def equals(obj: Any): Boolean = {
    if (obj == null || !obj.isInstanceOf[GhantDiagram]) {
      false
    } else {
      val other = obj.asInstanceOf[GhantDiagram]
      (computationDiagram == other.computationDiagram) && (transferDiagram == other.transferDiagram)
    }
  }

  override def toString: String = Show[GhantDiagram].show(this)
}

object GhantDiagram {

  implicit val diagramShow: Show[GhantDiagram] = diagram => {
    val computationsOnNodes = diagram.computationDiagram.processors.toList.sortBy(_._1)
    val transfersOnNodes = diagram.transferDiagram.processors.toList.sortBy(_._1)

    (computationsOnNodes zip transfersOnNodes)
      .map { case ((id, computations), (_, transfers)) =>
        val computatinPrefix = f"comput $id%2s"
        val transferPrefix = f"transf $id%2s"
        val computationRepr = computations.map(opt => opt.map(task => f"${task.node.id}%10s").getOrElse("..........")).mkString("|", "|", "|")
        val transferRepr = transfers.map(opt => opt.map(transfer => f"${transfer.data.source}%2s->${transfer.data.target}%2s(${transfer.target}%2s)").getOrElse("..........")).mkString("|", "|", "|")
        s"$computatinPrefix: $computationRepr\n$transferPrefix: $transferRepr"
      }
      .mkString("\n")
  }
}
