package com.yevhenii.cluster.punner.server.modeling

import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.{ExecutionPlanner, NodeLog, QueueCreator, Waiting, Work, Working}
import org.scalatest.{Matchers, WordSpec}

class ExecutionPlannerSpec extends WordSpec with Matchers {

  val taskGraph = OrientedGraph(
    Node("1", 5) ::
      Node("2", 4) ::
      Node("3", 2) ::
      Node("4", 20) ::
      Node("5", 2) ::
      Node("6", 6) ::
      Node("7", 1) ::
      Node("8", 3) ::
      Node("9", 5) ::
      OrientedEdge("1-5", 2, "1", "5") :: // 1 --> 5
      OrientedEdge("1-8", 3, "1", "8") :: // 1 --> 8
      OrientedEdge("2-5", 1, "2", "5") :: // 2 --> 5
      OrientedEdge("2-6", 2, "2", "6") :: // 2 --> 6
      OrientedEdge("2-8", 1, "2", "8") :: // 2 --> 8
      OrientedEdge("3-7", 4, "3", "7") :: // 3 --> 7
      OrientedEdge("5-8", 3, "5", "8") :: // 5 --> 8
      OrientedEdge("5-9", 2, "5", "9") :: // 5 --> 9
      OrientedEdge("6-9", 1, "6", "9") :: // 6 --> 9
      OrientedEdge("7-8", 4, "7", "8") :: // 7 --> 8
      OrientedEdge("7-9", 2, "7", "9") :: // 7 --> 9
      Nil
  )

  val systemGraph = NonOrientedGraph(
    Node("1", 1) ::
      Node("2", 2) ::
      Node("3", 2) ::
      Node("4", 1) ::
      NonOrientedEdge("1-2", 1, "1", "2") :: // 1 -- 2
      NonOrientedEdge("2-3", 1, "2", "3") :: // 2 -- 3
      NonOrientedEdge("4-1", 2, "4", "1") :: // 4 -- 1
      NonOrientedEdge("4-2", 2, "4", "2") :: // 4 -- 2
      NonOrientedEdge("4-3", 2, "4", "3") :: // 4 -- 3
      Nil
  )

  val queueCreator: OrientedGraph => List[Node] = task => QueueCreator.createQueueBasedOnCriticalPath(task).map(_._1)

  "ExecutionPlanner.planExecutionByConnectivity" should {
    "work correctly for empty graph of task" in {
      ExecutionPlanner.planExecutionByConnectivity(systemGraph, OrientedGraph(List.empty), queueCreator) shouldBe empty
    }

    "work correctly for empty system graph" in {
      ExecutionPlanner.planExecutionByConnectivity(NonOrientedGraph(List.empty), taskGraph, queueCreator) shouldBe empty
    }

    "work for trivial graph of task" in {
      val graph = Node("1", 1) :: Nil

      val expectedResult =
        NodeLog("4", List((1, Working(Work(0, 1, graph.head))))) ::
          List.tabulate(3)(i => NodeLog(s"${i + 1}", List((1, Waiting))))

      ExecutionPlanner.planExecutionByConnectivity(systemGraph, OrientedGraph(graph), queueCreator) shouldBe expectedResult
    }

    "work for trivial graph of system" in {
      val graph = Node("1", 1) :: Nil

      val expectedResult = Nil

      ExecutionPlanner.planExecutionByConnectivity(NonOrientedGraph(graph), taskGraph, queueCreator) shouldBe expectedResult
    }

    "create correct queue from example" in {
      val expected = Nil

      val actual = ExecutionPlanner.planExecutionByConnectivity(systemGraph, taskGraph, queueCreator)

      actual should contain theSameElementsInOrderAs expected
    }
  }
}
