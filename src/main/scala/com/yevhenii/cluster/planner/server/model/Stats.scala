package com.yevhenii.cluster.planner.server.model

case class Stats(
  queue: String,
  planning: String,
  size: Int,
  correlation: Double,
  time: Int,
  speedup: Double,
  efficiency: Double,
  algorithmEfficiency: Double
)
