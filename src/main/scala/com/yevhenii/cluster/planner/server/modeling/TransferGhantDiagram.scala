package com.yevhenii.cluster.planner.server.modeling

import cats.Show
import com.yevhenii.cluster.planner.server.model.{NonOrientedGraph, OrientedEdge}
import com.yevhenii.cluster.planner.server.graphs.GraphOps.Implicits._
import com.yevhenii.cluster.planner.server.modeling.TransferGhantDiagram.TransferringData

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class TransferGhantDiagram(systemGraph: NonOrientedGraph) {

  val processors: mutable.Map[String, ListBuffer[Option[TransferringData]]] =
    mutable.Map(systemGraph.nodes.map(_.id -> ListBuffer.empty[Option[TransferringData]]): _*)

  def transferTo(from: String, to: String, data: OrientedEdge, atLeastStartingFrom: Int): Int = {
    if (from == to) {
      atLeastStartingFrom
    } else {
      val shortestPath = systemGraph.findShortestPathInNodes(from, to, data.weight)

      shortestPath.init.zip(shortestPath.tail).foldLeft(atLeastStartingFrom) { (startFrom, pairOfNodes) =>
        val (from, to) = pairOfNodes
        scheduleDirectTransfer(from, to, data, startFrom).get
      }
    }
  }

  def scheduleDirectTransfer(from: String, to: String, data: OrientedEdge, atLeastStartingFrom: Int): Option[Int] = {
    val edgeOpt = systemGraph.edges.find(edge => (edge.source == from && edge.target == to) || (edge.source == to && edge.target == from))

    edgeOpt.map { edge =>
      val minStartTime = math.max(math.max(processors(from).size, processors(to).size), atLeastStartingFrom)

      for (_ <- processors(from).size until minStartTime) {
        processors(from).append(None)
      }

      for (_ <- processors(to).size until minStartTime) {
        processors(to).append(None)
      }

      val timeToTransfer = math.ceil(data.weight.toDouble / edge.weight).toInt

      val transfer = TransferringData(data, from, to)
      for (_ <- 1 to timeToTransfer) {
        processors(from).append(Some(transfer))
        processors(to).append(Some(transfer))
      }

      minStartTime + timeToTransfer
    }
  }

  override def equals(obj: Any): Boolean = {
    if (obj != null && obj.isInstanceOf[TransferGhantDiagram]) {
      val other = obj.asInstanceOf[TransferGhantDiagram]
      this.processors == other.processors
    } else {
      false
    }
  }

  override def toString: String = Show[TransferGhantDiagram].show(this)
}

object TransferGhantDiagram {
  case class TransferringData(data: OrientedEdge, source: String, target: String)

  implicit val show: Show[TransferGhantDiagram] = diagram => {
    diagram.processors.toList
      .sortBy(_._1)
      .map { case (id, queue) =>
        val queueStr = queue.map(opt => opt.map(transfer => s"${transfer.data.source}->${transfer.data.target}(${transfer.target})").getOrElse("...")).mkString("|", "|", "|")
        s"$id: $queueStr"
      }
      .mkString("\n")
  }
}
