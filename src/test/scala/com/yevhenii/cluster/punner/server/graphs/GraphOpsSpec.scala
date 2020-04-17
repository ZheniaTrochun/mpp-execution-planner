package com.yevhenii.cluster.punner.server.graphs

import com.yevhenii.cluster.planner.server.graphs.GraphOps
import com.yevhenii.cluster.planner.server.model._
import org.scalatest.{Matchers, WordSpec}

class GraphOpsSpec extends WordSpec with Matchers {
  "GraphOps.checkGraphForConnectivity" should {

    "return `true` for empty graph" in {
      GraphOps.checkGraphForConnectivity(List.empty) shouldBe true
    }

    "return `true` for graph with one node" in {
      val graph = Node("1", "1", "1") :: Nil
      GraphOps.checkGraphForConnectivity(graph) shouldBe true
    }

    "return `true` for simple fully connected graph" in {
      val graph =
        Node("1", "1-2", "2") ::
        NonOrientedEdge("1-2", "[1]", "1", "1", "2") ::
        Node("2", "2-3", "3") ::
        NonOrientedEdge("2-3", "[2]", "2", "2", "3") ::
        Node("3", "3-4", "4") ::
        NonOrientedEdge("3-1", "[3]", "3", "3", "1") ::
        Nil

      GraphOps.checkGraphForConnectivity(graph) shouldBe true
    }

    "return `true` for fully connected graph" in {
      val graph =
        Node("1", "1-2", "2") ::
          NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", "3") ::
          NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", "4") ::
          NonOrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", "5") ::
          NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", "6") ::
          NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", "7") ::
          Nil

      GraphOps.checkGraphForConnectivity(graph) shouldBe true
    }

    "return `true` for fully connected graph 2" in {
      val graph =
        Node("1", "1-2", "2") ::
        NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
        Node("2", "2-3", "3") ::
        NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
        Node("3", "3-4", "4") ::
        NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // 3 --> 1
        NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // duplicated edge
        NonOrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
        Node("4", "4-5", "5") ::
        NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
        Node("5", "5-6", "6") ::
        NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
        Node("6", "6-7", "7") ::
        NonOrientedEdge("6-1", "[6]", "6", "6", "1") :: // 6 --> 1
        NonOrientedEdge("1-6", "[7]", "7", "1", "6") :: // 1 --> 6
        Nil

      GraphOps.checkGraphForConnectivity(graph) shouldBe true
    }

    "return `false` for simple not fully connected graph" in {
      val graph =
        Node("1", "1-2", "2") ::
          Node("2", "2-3", "3") ::
          Node("3", "3-4", "4") ::
          Nil

      GraphOps.checkGraphForConnectivity(graph) shouldBe false
    }

    "return `false` for not fully connected graph" in {
      val graph =
        Node("1", "1-2", "2") ::
          NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", "3") ::
          NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", "4") ::
          Node("4", "4-5", "5") ::
          NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", "6") ::
          NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", "7") ::
          Nil

      GraphOps.checkGraphForConnectivity(graph) shouldBe false
    }

    "return `false` for not fully connected graph 2" in {
      val graph =
        Node("1", "1-2", "2") ::
          NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", "3") ::
          NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", "4") ::
          NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // 3 --> 1
          NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // duplicated edge
          Node("4", "4-5", "5") ::
          NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", "6") ::
          NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", "7") ::
          NonOrientedEdge("6-4", "[6]", "6", "6", "4") :: // 6 --> 4
          NonOrientedEdge("4-6", "[7]", "7", "4", "6") :: // 4 --> 6
          Nil

      GraphOps.checkGraphForConnectivity(graph) shouldBe false
    }
  }
}
