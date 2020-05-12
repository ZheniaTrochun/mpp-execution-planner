package com.yevhenii.cluster.planner.server.modeling

import com.yevhenii.cluster.planner.server.model.Node

case class Work(startedAt: Int, amount: Int, task: Node)
