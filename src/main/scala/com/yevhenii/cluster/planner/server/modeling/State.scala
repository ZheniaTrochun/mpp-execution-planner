package com.yevhenii.cluster.planner.server.modeling

sealed trait State {
  val schedulable: Boolean
}

sealed trait InWork extends State {
  override val schedulable: Boolean = false
  val work: Work

  def isFinished(time: Int, computingPower: Int): Boolean = {
    val timeDiff = time - work.startedAt
    val computedAmount = timeDiff * computingPower

    computedAmount > work.amount
  }
}

sealed trait InTransfer extends State {
  val transfer: Transfer

  def isTransferFinished(time: Int, linkSize: Int): Boolean = {
    val timeDiff = time - transfer.startedAt
    val computedAmount = timeDiff * linkSize

    computedAmount > transfer.amount
  }
}

case object Waiting extends State {
  override val schedulable: Boolean = true
}

case class Transferring(transfer: Transfer) extends InTransfer {
  override val schedulable: Boolean = true
}

case class Working(work: Work) extends InWork

case class WorkingAndTransferring(transfer: Transfer, work: Work) extends InWork with InTransfer