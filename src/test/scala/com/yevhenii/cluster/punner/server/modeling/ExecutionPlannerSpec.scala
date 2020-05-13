package com.yevhenii.cluster.punner.server.modeling

import cats.Show
import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.{ComputingGhantDiagram, ExecutionPlanner, NodeLog, QueueCreator, Waiting, Work, Working}
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
      OrientedEdge("9-7", 2, "9", "7") :: // 9 --> 7
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
      val expected = new ComputingGhantDiagram(systemGraph.nodes)

      ExecutionPlanner.planExecutionByConnectivity(systemGraph, OrientedGraph(List.empty), queueCreator) shouldBe expected
    }

    "work correctly for empty system graph" in {
      val expected = new ComputingGhantDiagram(List.empty)

      ExecutionPlanner.planExecutionByConnectivity(NonOrientedGraph(List.empty), taskGraph, queueCreator) shouldBe expected
    }

    "work for trivial graph of task" in {
      val graph = Node("1", 1) :: Nil

      val expectedResult = new ComputingGhantDiagram(systemGraph.nodes)
      expectedResult.schedule("2", graph.head, 0)

      ExecutionPlanner.planExecutionByConnectivity(systemGraph, OrientedGraph(graph), queueCreator) shouldBe expectedResult
    }

    "work for trivial graph of system" in {
      val graph = Node("1", 1) :: Nil

      // queue of tasks: 4 -> 2 -> 1 -> 6 -> 5 -> 9 -> 3 -> 7 -> 8
      val expectedResult = new ComputingGhantDiagram(graph)
      expectedResult.schedule("1", taskGraph.nodes(3), 0)
      expectedResult.schedule("1", taskGraph.nodes(1), 20)
      expectedResult.schedule("1", taskGraph.nodes(0), 24)
      expectedResult.schedule("1", taskGraph.nodes(5), 29)
      expectedResult.schedule("1", taskGraph.nodes(4), 35)
      expectedResult.schedule("1", taskGraph.nodes(8), 37)
      expectedResult.schedule("1", taskGraph.nodes(2), 42)
      expectedResult.schedule("1", taskGraph.nodes(6), 44)
      expectedResult.schedule("1", taskGraph.nodes(7), 45)

      println(Show[ComputingGhantDiagram].show(expectedResult))

      ExecutionPlanner.planExecutionByConnectivity(NonOrientedGraph(graph), taskGraph, queueCreator) shouldBe expectedResult
    }

    "create correct queue from example" in {
      // todo form expected result
      val expected = new ComputingGhantDiagram(systemGraph.nodes)

      val actual = ExecutionPlanner.planExecutionByConnectivity(systemGraph, taskGraph, queueCreator)

      actual shouldBe expected
    }
  }
}
