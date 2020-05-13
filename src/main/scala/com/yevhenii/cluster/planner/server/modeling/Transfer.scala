package com.yevhenii.cluster.planner.server.modeling

case class Transfer(
  from: String,
  to: String,
  finalTarget: String,
  amount: Int,
  linkLabel: String,
  startedAt: Int,
  linkSize: Int,
  dataForTaskId: String
)