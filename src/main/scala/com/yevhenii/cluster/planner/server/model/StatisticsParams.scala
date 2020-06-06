package com.yevhenii.cluster.planner.server.model

case class StatisticsParams(
  startingSize: Int,
  sizeLimit: Int,
  connectivityStart: Double,
  connectivityLimit: Double,
  connectivityStep: Double
)
