package com.yevhenii.cluster.planner.server.model

case class StatisticsParams(
  maxSizeMultiplier: Int,
  connectivityStart: Double,
  connectivityLimit: Double,
  connectivityStep: Double
)
