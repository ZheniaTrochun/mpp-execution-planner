package com.yevhenii.cluster.planner.server.model

import GraphParameters._

case class GraphParameters(
  minimalNodeWeight: Int = DefaultMinNodeWeight,
  maximumNodeWeight: Int = DefaultMaxNodeWeight,
  numberOfNodes: Int,
  correlation: Double,
  minimalEdgeWeight: Int = DefaultMinNodeWeight,
  maximumEdgeWeight: Int = DefaultMaxEdgeWeight
)

object GraphParameters {
  val DefaultMinNodeWeight = 1
  val DefaultMaxNodeWeight = 10

  val DefaultMinEdgeWeight = 1
  val DefaultMaxEdgeWeight = 10
}