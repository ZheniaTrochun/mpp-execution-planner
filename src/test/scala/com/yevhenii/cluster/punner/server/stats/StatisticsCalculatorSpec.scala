package com.yevhenii.cluster.punner.server.stats

import com.yevhenii.cluster.planner.server.model.{Node, NonOrientedEdge, NonOrientedGraph, StatisticsParams}
import com.yevhenii.cluster.planner.server.statistics.StatisticsCalculator
import org.scalatest.{AsyncWordSpec, Matchers}

import scala.concurrent.ExecutionContext

class StatisticsCalculatorSpec extends AsyncWordSpec with Matchers {

  val systemGraph = NonOrientedGraph(
    Node("1", 1) ::
      Node("2", 2) ::
      Node("3", 2) ::
      Node("4", 1) ::
      Node("5", 1) ::
      NonOrientedEdge("1-2", 1, "1", "2") :: // 1 -- 2
      NonOrientedEdge("2-3", 1, "2", "3") :: // 2 -- 3
      NonOrientedEdge("4-1", 2, "4", "1") :: // 4 -- 1
      NonOrientedEdge("4-2", 2, "4", "2") :: // 4 -- 2
      NonOrientedEdge("4-3", 2, "4", "3") :: // 4 -- 3
      NonOrientedEdge("4-5", 2, "4", "5") :: // 4 -- 5
      Nil
  )

  implicit val ec = ExecutionContext.Implicits.global

  "StatisticsCalculator.calculateStatistics" should {
    "never return time 0" in {

      val params = StatisticsParams(4, 0.3, 1.0, 0.1)
      StatisticsCalculator.calculateStatistics(systemGraph, params).map(_.exists(_.time == 0) shouldBe false)
    }
  }
}
