package com.yevhenii.cluster.punner.server.modeling

import com.yevhenii.cluster.planner.server.model._
import com.yevhenii.cluster.planner.server.modeling.Queues
import org.scalatest.{Matchers, WordSpec}

class QueuesSpec extends WordSpec with Matchers {

  val graph = OrientedGraph(
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

  "QueueCreator.CriticalPathBased" should {
    "work correctly for empty graph" in {
      Queues.CriticalPathBased.applyExtended(OrientedGraph(Nil)) shouldBe empty
    }

    "work for trivial graph" in {
      val graph = Node("1", 1) :: Nil
      Queues.CriticalPathBased.applyExtended(OrientedGraph(graph)) shouldBe List((graph.head, graph))
    }

    "create correct queue from example" in {
      val expected =
        Node("4", 20)  -> 20 ::
          Node("2", 4) -> 15 ::
          Node("1", 5) -> 12 ::
          Node("6", 6) -> 11 ::
          Node("3", 2) -> 8  ::
          Node("5", 2) -> 7  ::
          Node("7", 1) -> 6  ::
          Node("9", 5) -> 5  ::
          Node("8", 3) -> 3  ::
          Nil

      val actual = Queues.CriticalPathBased.applyExtended(graph).map { case (node, path) => node -> path.map(_.weight).sum }

      actual should contain theSameElementsInOrderAs expected
    }
  }

  "QueueCreator.NodesOnCriticalPathBased.applyExtended" should {
    "work correctly for empty graph" in {
      Queues.NodesOnCriticalPathBased.applyExtended(OrientedGraph(Nil)) shouldBe empty
    }

    "work for trivial graph" in {
      val graph = Node("1", 1) :: Nil
      Queues.NodesOnCriticalPathBased.applyExtended(OrientedGraph(graph)) shouldBe List((graph.head, graph))
    }

    "create correct queue from example" in {
      val expected =
        Node("1", 5)     -> 3 ::
          Node("5", 2)   -> 2 ::
          Node("8", 3)   -> 1 ::
          Node("2", 4)   -> 3 ::
          Node("3", 2)   -> 3 ::
          Node("6", 6)   -> 2 ::
          Node("7", 1)   -> 2 ::
          Node("4", 20)  -> 1 ::
          Node("9", 5)   -> 1 ::
          Nil

      val actual = Queues.NodesOnCriticalPathBased.applyExtended(graph).map { case (node, path) => node -> path.length }

      actual should contain theSameElementsInOrderAs expected
    }
  }

  "QueueCreator.ConnectivityBased.applyExtended" should {
    "work correctly for empty graph" in {
      Queues.ConnectivityBased.applyExtended(OrientedGraph(Nil)) shouldBe empty
    }

    "work for trivial graph" in {
      val graph = Node("1", 1) :: Nil
      Queues.ConnectivityBased.applyExtended(OrientedGraph(graph)) shouldBe List((graph.head, (0, 1)))
    }

    "create correct queue from example" in {
      val expected =
        Node("5", 2)     -> (4, 2) ::
          Node("8", 3)   -> (4, 1) ::
          Node("2", 4)   -> (3, 3) ::
          Node("7", 1)   -> (3, 2) ::
          Node("9", 5)   -> (3, 1) ::
          Node("1", 5)   -> (2, 3) ::
          Node("6", 6)   -> (2, 2) ::
          Node("3", 2)   -> (1, 3) ::
          Node("4", 20)  -> (0, 1) ::
          Nil

      val actual = Queues.ConnectivityBased.applyExtended(graph)

      actual should contain theSameElementsInOrderAs expected
    }
  }
}
