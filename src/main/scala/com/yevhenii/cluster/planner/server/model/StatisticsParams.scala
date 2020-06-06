package com.yevhenii.cluster.planner.server.model

case class StatisticsParams(
  maxSizeMultiplier: Int,
  correlationStart: Double,
  correlationLimit: Double,
  correlationStep: Double
)
