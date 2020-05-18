package com.yevhenii.cluster.planner.server.dto

import cats.instances.list._
import cats.Show
import com.yevhenii.cluster.planner.server.modeling.GhantDiagram

case class Diagram(entries: List[DiagramEntry])

sealed trait DiagramEntry {
  val `type`: String
}

object DiagramEntry {
  implicit val show: Show[DiagramEntry] = {
    case computing: DiagramComputingEntry => computing.toString
    case transfer: DiagramTransferringEntry => transfer.toString
  }
}

case class DiagramComputingEntry(node: String, task: String, start: Int, duration: Int, `type`: String = "computing") extends DiagramEntry

case class DiagramTransferringEntry(node: String, edge: String, target: String, start: Int, duration: Int, `type`: String = "transfer") extends DiagramEntry

object Diagram {

  implicit val show: Show[Diagram] = diagram => Show[List[DiagramEntry]].show(diagram.entries)

  def from(diagram: GhantDiagram): Diagram = {
    val computationEntries = diagram.computationDiagram.processors
      .flatMap { case (nodeId, computations) =>
        computations.zipWithIndex
          .filterNot(_._1.isEmpty)
          .map { case (computingOpt, id) => (computingOpt.get, id) }
          .groupBy { case (computationEntry, _) => computationEntry.node.id }
          .map { case (taskId, computations) =>
            val duration = computations.size
            val start = computations.map(_._2).min
            DiagramComputingEntry(nodeId, taskId, start, duration)
          }
      }
      .toList

    val transferEntries = diagram.transferDiagram.processors
      .flatMap { case (nodeId, transfers) =>
        transfers.zipWithIndex
          .filterNot(_._1.isEmpty)
          .map { case (entryOpt, tact) => (entryOpt.get, tact) }
          .filterNot { case (entry, _) => entry.target == nodeId }
          .groupBy { case (transferEntry, _) => (transferEntry.data.id, transferEntry.data.target) }
          .map { case ((id, target), entries) =>
            val duration = entries.size
            val start = entries.map(_._2).min
            DiagramTransferringEntry(nodeId, id, target, start, duration)
          }
      }
      .toList

    Diagram(transferEntries ::: computationEntries)
  }
}
