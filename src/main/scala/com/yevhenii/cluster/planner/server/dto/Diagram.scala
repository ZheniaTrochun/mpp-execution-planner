package com.yevhenii.cluster.planner.server.dto

import com.yevhenii.cluster.planner.server.modeling.GhantDiagram

case class Diagram(entries: List[DiagramEntry])

sealed trait DiagramEntry {
  val `type`: String
}

case class DiagramComputingEntry(node: String, task: String, start: Int, duration: Int) extends DiagramEntry {
  val `type`: String = "computing"
}

case class DiagramTransferringEntry(node: String, edge: String, target: String, start: Int, duration: Int) extends DiagramEntry {
  val `type`: String = "transfer"
}

object Diagram {
  def from(diagram: GhantDiagram): Diagram = {
    val computationEntries = diagram.computationDiagram.processors
      .flatMap { case (nodeId, computations) =>
        computations.zipWithIndex
          .filterNot(_._1.isEmpty)
          .foldLeft(Map.empty[String, DiagramComputingEntry]) { (acc, entry) =>
            val (computationEntry, tact) = entry

            computationEntry.fold(acc) { computation =>
              val existingOpt = acc.get(computation.node.id)
              val updated = existingOpt match {
                case Some(existing) => existing.copy(duration = existing.duration + 1)
                case None => DiagramComputingEntry(nodeId, computation.node.id, tact, 1)
              }
              acc + (updated.node -> updated)
            }
          }
          .values
      }
      .toList

    val transferEntries = diagram.transferDiagram.processors
      .flatMap { case (nodeId, transfers) =>
        transfers.zipWithIndex
          .filterNot(_._1.isEmpty)
          .map { case (entryOpt, tact) => (entryOpt.get, tact) }
          .filterNot { case (entry, _) => entry.target == nodeId }
          .foldLeft(Map.empty[(String, String), DiagramTransferringEntry]) { (acc, entry) =>
            val (transferEntry, tact) = entry

            val key = (transferEntry.data.label, transferEntry.target)
            val existingOpt = acc.get(key)
            val updated = existingOpt match {
              case Some(existing) => existing.copy(duration = existing.duration + 1)
              case None => DiagramTransferringEntry(nodeId, transferEntry.data.label, transferEntry.target, tact, 1)
            }
            acc + (key -> updated)
          }
          .values
      }
      .toList

    Diagram(transferEntries ::: computationEntries)
  }
}
