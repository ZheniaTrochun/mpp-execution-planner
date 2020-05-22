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

      // todo: test fails because one of transfers scheduled earlier then expected

      val transferDuration = math.ceil(data.weight.toDouble / edge.weight).toInt
      val transfer = TransferringData(data, from, to)

      val slidingIteratorOfFirst = processors(from).zipWithIndex.drop(atLeastStartingFrom).sliding(transferDuration)
      val slidingIteratorOfSecond = processors(to).zipWithIndex.drop(atLeastStartingFrom).sliding(transferDuration)

      val indexOfStartOpt =
        (slidingIteratorOfFirst zip slidingIteratorOfSecond)
          .find { case (first, second) => first.forall(_._1.isEmpty) && second.forall(_._1.isEmpty) }
          .filter { case (first, second) => first.length == transferDuration && second.length == transferDuration }
          .map { case (first, _) => first.head._2 }

      indexOfStartOpt match {
        case Some(startingPoint) =>
          for (i <- startingPoint until (startingPoint + transferDuration)) {
            processors(from)(i) = Some(transfer)
            processors(to)(i) = Some(transfer)
          }

          startingPoint + transferDuration

        case None =>
          val minStartTime = math.max(math.max(processors(from).size, processors(to).size), atLeastStartingFrom)

          for (_ <- processors(from).size until minStartTime) {
            processors(from).append(None)
          }

          for (_ <- processors(to).size until minStartTime) {
            processors(to).append(None)
          }

          for (_ <- 1 to transferDuration) {
            processors(from).append(Some(transfer))
            processors(to).append(Some(transfer))
          }

          minStartTime + transferDuration
      }
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
