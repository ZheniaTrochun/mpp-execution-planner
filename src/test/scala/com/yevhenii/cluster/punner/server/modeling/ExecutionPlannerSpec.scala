package com.yevhenii.cluster.punner.server.modeling

import cats.Show
import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.{ExecutionPlanner, GhantDiagram, QueueCreator}
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
      val expected = new GhantDiagram(systemGraph)

      ExecutionPlanner.ConnectivityPlanner.apply(systemGraph, OrientedGraph(List.empty), queueCreator) shouldBe expected
    }

    "work correctly for empty system graph" in {
      val expected = new GhantDiagram(NonOrientedGraph(List.empty))

      ExecutionPlanner.ConnectivityPlanner.apply(NonOrientedGraph(List.empty), taskGraph, queueCreator) shouldBe expected
    }

    "work for trivial graph of task" in {
      val graph = Node("1", 1) :: Nil

      val expectedResult = new GhantDiagram(systemGraph)
      expectedResult.schedule("2", graph.head, 0)

      ExecutionPlanner.ConnectivityPlanner.apply(systemGraph, OrientedGraph(graph), queueCreator) shouldBe expectedResult
    }

    "work for trivial graph of system" in {
      val graph = Node("1", 1) :: Nil
      val systemGraph = NonOrientedGraph(graph)

      // queue of tasks: 4 -> 2 -> 1 -> 6 -> 5 -> 9 -> 3 -> 7 -> 8
      val expectedResult = new GhantDiagram(systemGraph)
      expectedResult.schedule("1", taskGraph.nodes(3), 0)
      expectedResult.schedule("1", taskGraph.nodes(1), 20)
      expectedResult.schedule("1", taskGraph.nodes(0), 24)
      expectedResult.schedule("1", taskGraph.nodes(5), 29)
      expectedResult.schedule("1", taskGraph.nodes(4), 35)
      expectedResult.schedule("1", taskGraph.nodes(8), 37)
      expectedResult.schedule("1", taskGraph.nodes(2), 42)
      expectedResult.schedule("1", taskGraph.nodes(6), 44)
      expectedResult.schedule("1", taskGraph.nodes(7), 45)

      ExecutionPlanner.ConnectivityPlanner.apply(systemGraph, taskGraph, queueCreator) shouldBe expectedResult
    }

    "create correct queue from example" in {
      val expected = new GhantDiagram(systemGraph)
      expected.schedule("1", taskGraph.nodes(0), 0)
      expected.schedule("1", taskGraph.nodes(4), 6)
      expected.schedule("2", taskGraph.nodes(3), 0)
      expected.schedule("2", taskGraph.nodes(8), 13)
      expected.schedule("2", taskGraph.nodes(6), 20)
      expected.schedule("2", taskGraph.nodes(7), 30)
      expected.schedule("3", taskGraph.nodes(2), 0)
      expected.schedule("4", taskGraph.nodes(1), 0)
      expected.schedule("4", taskGraph.nodes(5), 4)

      expected.scheduleDirectTransfer("4", "1", taskGraph.edges(2), 5)
      expected.scheduleDirectTransfer("1", "2", taskGraph.edges(7), 10)
      expected.scheduleDirectTransfer("4", "2", taskGraph.edges(8), 12)
      expected.scheduleDirectTransfer("3", "2", taskGraph.edges(5), 16)
      expected.scheduleDirectTransfer("1", "4", taskGraph.edges(1), 21)
      expected.scheduleDirectTransfer("4", "2", taskGraph.edges(1), 23)
      expected.scheduleDirectTransfer("4", "2", taskGraph.edges(4), 25)
      expected.scheduleDirectTransfer("1", "4", taskGraph.edges(6), 26)
      expected.scheduleDirectTransfer("4", "2", taskGraph.edges(6), 28)

      val actual = ExecutionPlanner.ConnectivityPlanner.apply(systemGraph, taskGraph, queueCreator)

      actual shouldBe expected
    }
  }

  "ExecutionPlanner.planExecutionByNeighbor" should {
    "work correctly for empty graph of task" in {
      val expected = new GhantDiagram(systemGraph)

      ExecutionPlanner.CloseNeighborPlanner.apply(systemGraph, OrientedGraph(List.empty), queueCreator) shouldBe expected
    }

    "work correctly for empty system graph" in {
      val expected = new GhantDiagram(NonOrientedGraph(List.empty))

      ExecutionPlanner.CloseNeighborPlanner.apply(NonOrientedGraph(List.empty), taskGraph, queueCreator) shouldBe expected
    }

    "work for trivial graph of task" in {
      val graph = Node("1", 1) :: Nil

      val expectedResult = new GhantDiagram(systemGraph)
      expectedResult.schedule("2", graph.head, 0)

      ExecutionPlanner.CloseNeighborPlanner.apply(systemGraph, OrientedGraph(graph), queueCreator) shouldBe expectedResult
    }

    "work for trivial graph of system" in {
      val graph = Node("1", 1) :: Nil
      val systemGraph = NonOrientedGraph(graph)

      // queue of tasks: 4 -> 2 -> 1 -> 6 -> 5 -> 9 -> 3 -> 7 -> 8
      val expectedResult = new GhantDiagram(systemGraph)
      expectedResult.schedule("1", taskGraph.nodes(3), 0)
      expectedResult.schedule("1", taskGraph.nodes(1), 20)
      expectedResult.schedule("1", taskGraph.nodes(0), 24)
      expectedResult.schedule("1", taskGraph.nodes(5), 29)
      expectedResult.schedule("1", taskGraph.nodes(4), 35)
      expectedResult.schedule("1", taskGraph.nodes(8), 37)
      expectedResult.schedule("1", taskGraph.nodes(2), 42)
      expectedResult.schedule("1", taskGraph.nodes(6), 44)
      expectedResult.schedule("1", taskGraph.nodes(7), 45)

      ExecutionPlanner.CloseNeighborPlanner.apply(systemGraph, taskGraph, queueCreator) shouldBe expectedResult
    }

    "create correct queue from example" ignore {
      val expected = new GhantDiagram(systemGraph)
      expected.schedule("1", taskGraph.nodes(0), 0)
      expected.schedule("1", taskGraph.nodes(4), 5)
      expected.schedule("1", taskGraph.nodes(8), 11)
      expected.schedule("2", taskGraph.nodes(3), 0)
      expected.schedule("3", taskGraph.nodes(2), 0)
      expected.schedule("3", taskGraph.nodes(6), 18)
      expected.schedule("3", taskGraph.nodes(7), 19)
      expected.schedule("4", taskGraph.nodes(1), 0)
      expected.schedule("4", taskGraph.nodes(5), 4)

      expected.scheduleDirectTransfer("4", "1", taskGraph.edges(2), 4)
      expected.scheduleDirectTransfer("1", "4", taskGraph.edges(1), 5)
      expected.scheduleDirectTransfer("4", "3", taskGraph.edges(1), 7)
      expected.scheduleDirectTransfer("4", "3", taskGraph.edges(4), 9)
      expected.scheduleDirectTransfer("4", "1", taskGraph.edges(8), 10)
      expected.scheduleDirectTransfer("1", "4", taskGraph.edges(6), 11)
      expected.scheduleDirectTransfer("4", "3", taskGraph.edges(6), 13)
      expected.scheduleDirectTransfer("1", "4", taskGraph.edges(10), 16)
      expected.scheduleDirectTransfer("4", "3", taskGraph.edges(10), 17)

      val actual = ExecutionPlanner.CloseNeighborPlanner.apply(systemGraph, taskGraph, queueCreator)

      actual shouldBe expected
    }
  }

  "create correct queue from example 2" ignore {
    val system = NonOrientedGraph(
      Node("5", 1) ::
        Node("4", 1) ::
        Node("3", 1) ::
        Node("2", 1) ::
        Node("1", 1) ::
        NonOrientedEdge("1-2", 1, "1", "2") ::
        NonOrientedEdge("2-3", 2, "2", "3") ::
        NonOrientedEdge("3-4", 2, "3", "4") ::
        NonOrientedEdge("4-5", 1, "4", "5") ::
        Nil
    )

    val task = OrientedGraph(
      Node("1", 3) ::
        Node("2", 7) ::
        Node("3", 5) ::
        Node("4", 3) ::
        Node("5", 4) ::
        Node("6", 4) ::
        Node("7", 5) ::
        Node("8", 3) ::
        Node("9", 4) ::
        Node("10", 1) ::
        OrientedEdge("1-3", 1, "1", "3") ::
        OrientedEdge("1-7", 3, "1", "7") ::
        OrientedEdge("2-3", 1, "2", "3") ::
        OrientedEdge("2-9", 2, "2", "9") ::
        OrientedEdge("2-10", 2, "2", "10") ::
        OrientedEdge("3-4", 3, "3", "4") ::
        OrientedEdge("3-5", 1, "3", "5") ::
        OrientedEdge("3-9", 3, "3", "9") ::
        OrientedEdge("4-6", 3, "4", "6") ::
        OrientedEdge("5-6", 1, "5", "6") ::
        OrientedEdge("5-8", 1, "5", "8") ::
        OrientedEdge("5-7", 1, "5", "7") ::
        OrientedEdge("7-10", 1, "7", "10") ::
        Nil
    )

    val expected = new GhantDiagram(system)

    val actual = ExecutionPlanner.CloseNeighborPlanner.apply(system, task, queueCreator)

    actual shouldBe expected
  }
}
