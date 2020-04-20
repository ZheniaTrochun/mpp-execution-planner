package com.yevhenii.cluster.punner.server.graphs

import com.yevhenii.cluster.planner.server.graphs.GraphOps
import com.yevhenii.cluster.planner.server.model._
import org.scalatest.{Matchers, WordSpec}

class GraphOpsSpec extends WordSpec with Matchers {
  "GraphOps.checkGraphForConnectivity" should {

    "return `true` for empty graph" in {
      GraphOps.checkGraphForConnectivity(NonOrientedGraph(List.empty)) shouldBe true
    }

    "return `true` for graph with one node" in {
      val graph = Node("1", "1", 1) :: Nil
      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe true
    }

    "return `true` for simple fully connected graph" in {
      val graph =
        Node("1", "1-2", 2) ::
        NonOrientedEdge("1-2", "[1]", "1", "1", "2") ::
        Node("2", "2-3", 3) ::
        NonOrientedEdge("2-3", "[2]", "2", "2", "3") ::
        Node("3", "3-4", 4) ::
        NonOrientedEdge("3-1", "[3]", "3", "3", "1") ::
        Nil

      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe true
    }

    "return `true` for fully connected graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          NonOrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil

      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe true
    }

    "return `true` for fully connected graph 2" in {
      val graph =
        Node("1", "1-2", 2) ::
        NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
        Node("2", "2-3", 3) ::
        NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
        Node("3", "3-4", 4) ::
        NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // 3 --> 1
        NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // duplicated edge
        NonOrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
        Node("4", "4-5", 5) ::
        NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
        Node("5", "5-6", 6) ::
        NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
        Node("6", "6-7", 7) ::
        NonOrientedEdge("6-1", "[6]", "6", "6", "1") :: // 6 --> 1
        NonOrientedEdge("1-6", "[7]", "7", "1", "6") :: // 1 --> 6
        Nil

      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe true
    }

    "return `false` for simple not fully connected graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Nil

      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe false
    }

    "return `false` for not fully connected graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil

      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe false
    }

    "return `false` for not fully connected graph 2" in {
      val graph =
        Node("1", "1-2", 2) ::
          NonOrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          NonOrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // 3 --> 1
          NonOrientedEdge("3-1", "[3]", "3", "3", "1") :: // duplicated edge
          Node("4", "4-5", 5) ::
          NonOrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          NonOrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          NonOrientedEdge("6-4", "[6]", "6", "6", "4") :: // 6 --> 4
          NonOrientedEdge("4-6", "[7]", "7", "4", "6") :: // 4 --> 6
          Nil

      GraphOps.checkGraphForConnectivity(NonOrientedGraph(graph)) shouldBe false
    }
  }

  "GraphOps.checkGraphForCycles" should {

    "return `true` for empty graph" in {
      GraphOps.checkGraphForCycles(OrientedGraph(List.empty)) shouldBe true
    }

    "return `true` for graph with one node" in {
      val graph = Node("1", "1", 1) :: Nil
      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe true
    }

    "return `true` for simple acyclic graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") ::
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") ::
          Node("3", "3-4", 4) ::
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe true
    }

    "return `true` for acyclic graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe true
    }

    "return `true` for acyclic graph 2" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          OrientedEdge("1-3", "[3]", "3", "1", "3") :: // 1 --> 3
          OrientedEdge("1-3", "[3]", "3", "1", "3") :: // duplicated edge
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Node("7", "7-8", 8) ::
          Node("8", "8-9", 9) ::
          OrientedEdge("8-8", "[10]", "10", "8", "9") :: // 8 --> 9
          Node("9", "9-10", 10) ::
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe true
    }

    "return `false` for simple cyclic graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-1", "[1]", "1", "1", "1") ::
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe false
    }

    "return `false` for simple cyclic graph 2" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") ::
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") ::
          Node("3", "3-4", 4) ::
          OrientedEdge("3-1", "[3]", "3", "3", "1") ::
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe false
    }

    "return `false` for cyclic graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("3-1", "[3]", "3", "3", "1") :: // 3 --> 1
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe false
    }

    "return `false` for cyclic graph 2" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          OrientedEdge("6-4", "[6]", "6", "6", "4") :: // 6 --> 4
          Nil

      GraphOps.checkGraphForCycles(OrientedGraph(graph)) shouldBe false
    }
  }

  "GraphOps.findInitialVertices" should {
    "work correctly for empty graph" in {
      GraphOps.findInitialVertices(OrientedGraph(List.empty)) shouldBe empty
    }

    "find initial vertex in trivial case" in {
      val singleNodeGraph = Node("1", "node1", 2) :: Nil
      GraphOps.findInitialVertices(OrientedGraph(singleNodeGraph)) shouldBe singleNodeGraph
    }

    "find all initial vertices in simple graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil

      val expected = Node("1", "1-2", 2) :: Node("4", "4-5", 5) :: Nil

      GraphOps.findInitialVertices(OrientedGraph(graph)) shouldBe expected
    }
  }

  "find all initial vertices in graph" in {
    val graph =
      Node("1", "1-2", 2) ::
        OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
        Node("2", "2-3", 3) ::
        OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
        Node("3", "3-4", 4) ::
        Node("4", "4-5", 5) ::
        OrientedEdge("4-3", "[7]", "7", "4", "3") :: // 4 --> 3
        OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
        Node("5", "5-6", 6) ::
        Node("6", "6-20", 20) ::
        Nil

    val expected = Node("1", "1-2", 2) :: Node("4", "4-5", 5) :: Node("6", "6-20", 20) :: Nil

    GraphOps.findInitialVertices(OrientedGraph(graph)) shouldBe expected
  }

  "GraphOps.findTerminalVertices" should {
    "work correctly for empty graph" in {
      GraphOps.findTerminalVertices(OrientedGraph(List.empty)) shouldBe empty
    }

    "find terminal vertex in trivial case" in {
      val singleNodeGraph = Node("1", "node1", 2) :: Nil
      GraphOps.findTerminalVertices(OrientedGraph(singleNodeGraph)) shouldBe singleNodeGraph
    }

    "find all terminal vertices in simple graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil

      val expected = Node("3", "3-4", 4) :: Node("6", "6-7", 7) :: Nil

      GraphOps.findTerminalVertices(OrientedGraph(graph)) shouldBe expected
    }

    "find all terminal vertices in graph" in {
      val graph =
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          OrientedEdge("4-3", "[7]", "7", "4", "3") :: // 4 --> 3
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          Node("6", "6-20", 20) ::
          Nil

      val expected = Node("3", "3-4", 4) :: Node("5", "5-6", 6) :: Node("6", "6-20", 20) :: Nil

      GraphOps.findTerminalVertices(OrientedGraph(graph)) shouldBe expected
    }
  }

  "GraphOps.findPaths" should {
    "work correctly for empty graph" in {
      GraphOps.findPaths(OrientedGraph(List.empty), Node("1", "1", 1), Node("2", "2", 2)) shouldBe empty
    }

    "work correctly if `from` is not in graph" in {
      val singleNodeGraph = OrientedGraph(
        Node("1", "node1", 2) :: Nil
      )

      GraphOps.findPaths(singleNodeGraph, Node("2", "2", 2), singleNodeGraph.nodes(0)) shouldBe empty
    }

    "work correctly if `target` is not in graph" in {
      val singleNodeGraph = OrientedGraph(
        Node("1", "node1", 2) :: Nil
      )

      GraphOps.findPaths(singleNodeGraph, singleNodeGraph.nodes(0), Node("2", "2", 2)) shouldBe empty
    }

    "should not do anything if graph has cycles" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") ::
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") ::
          Node("3", "3-4", 4) ::
          OrientedEdge("3-1", "[3]", "3", "3", "1") ::
          Nil
      )

      GraphOps.findPaths(graph, graph.nodes(0), graph.nodes(1)) shouldBe empty
    }

    "find path through graph in trivial case" in {
      val singleNodeGraph = OrientedGraph(Node("1", "node1", 2) :: Nil)
      GraphOps.findPaths(singleNodeGraph, singleNodeGraph.nodes.head, singleNodeGraph.nodes.head) shouldBe Some(Set(singleNodeGraph.nodes))
    }

    "find paths through graph in simple graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil
      )

      val expected1 = Some(Set(
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Node("5", "5-6", 6) ::
          Node("6", "6-7", 7) ::
          Nil
      ))

      val expected2 = Some(Set(
        Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Node("5", "5-6", 6) ::
          Nil
      ))

      GraphOps.findPaths(graph, graph.nodes.head, graph.nodes.last) shouldBe expected1
      GraphOps.findPaths(graph, graph.nodes.tail.head, graph.nodes.init.last) shouldBe expected2
    }

    "find paths through graph in graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          OrientedEdge("1-4", "[1]", "1", "1", "4") :: // 1 --> 4
          OrientedEdge("1-3", "[1]", "1", "1", "3") :: // 1 --> 3
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("2-4", "[2]", "2", "2", "4") :: // 2 --> 4
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          Nil
      )

      val expected1 = Some(Set(
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Nil,
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("4", "4-5", 5) ::
          Nil,
        Node("1", "1-2", 2) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Nil,
        Node("1", "1-2", 2) ::
          Node("4", "4-5", 5) ::
          Nil
      ))

      GraphOps.findPaths(graph, graph.nodes.head, graph.nodes.last) shouldBe expected1
    }

    "return `None` if there is no path between desired vertices" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil
      )

      GraphOps.findPaths(graph, graph.nodes.head, graph.nodes.last) shouldBe empty
    }
  }

  "GraphOps.findCriticalPath" should {
    "work correctly for empty graph" in {
      GraphOps.findCriticalPath(OrientedGraph(List.empty)) shouldBe empty
    }

    "should not do anything if graph has cycles" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") ::
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") ::
          Node("3", "3-4", 4) ::
          OrientedEdge("3-1", "[3]", "3", "3", "1") ::
          Nil
      )

      GraphOps.findCriticalPath(graph) shouldBe empty
    }

    "find critical path through graph in trivial case" in {
      val singleNodeGraph = OrientedGraph(Node("1", "node1", 2) :: Nil)
      GraphOps.findCriticalPath(singleNodeGraph) shouldBe singleNodeGraph.nodes
    }

    "find critical path through graph in simple graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil
      )

      val expected =
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Node("5", "5-6", 6) ::
          Node("6", "6-7", 7) ::
          Nil

      GraphOps.findCriticalPath(graph) shouldBe expected
    }

    "find critical path through graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          OrientedEdge("1-4", "[1]", "1", "1", "4") :: // 1 --> 4
          OrientedEdge("1-3", "[1]", "1", "1", "3") :: // 1 --> 3
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("2-4", "[2]", "2", "2", "4") :: // 2 --> 4
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          Nil
      )

      val expected =
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Nil

      GraphOps.findCriticalPath(graph) shouldBe expected
    }

    "find critical path through graph 2" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          OrientedEdge("1-4", "[1]", "1", "1", "4") :: // 1 --> 4
          OrientedEdge("1-3", "[1]", "1", "1", "3") :: // 1 --> 3
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("2-4", "[2]", "2", "2", "4") :: // 2 --> 4
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          Node("5", "5-20", 20) ::
          Nil
      )

      val expected =
        Node("5", "5-20", 20) ::
          Nil

      GraphOps.findCriticalPath(graph) shouldBe expected
    }
  }

  "GraphOps.findCriticalPathByNodesCount" should {
    "work correctly for empty graph" in {
      GraphOps.findCriticalPathByNodesCount(OrientedGraph(List.empty)) shouldBe empty
    }

    "should not do anything if graph has cycles" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") ::
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") ::
          Node("3", "3-4", 4) ::
          OrientedEdge("3-1", "[3]", "3", "3", "1") ::
          Nil
      )

      GraphOps.findCriticalPathByNodesCount(graph) shouldBe empty
    }

    "find critical path through graph in trivial case" in {
      val singleNodeGraph = OrientedGraph(Node("1", "node1", 2) :: Nil)
      GraphOps.findCriticalPathByNodesCount(singleNodeGraph) shouldBe singleNodeGraph.nodes
    }

    "find critical path through graph in simple graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil
      )

      val expected =
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Node("5", "5-6", 6) ::
          Node("6", "6-7", 7) ::
          Nil

      GraphOps.findCriticalPathByNodesCount(graph) shouldBe expected
    }

    "find critical path through graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          OrientedEdge("1-4", "[1]", "1", "1", "4") :: // 1 --> 4
          OrientedEdge("1-3", "[1]", "1", "1", "3") :: // 1 --> 3
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("2-4", "[2]", "2", "2", "4") :: // 2 --> 4
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          Nil
      )

      val expected =
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Nil

      GraphOps.findCriticalPathByNodesCount(graph) shouldBe expected
    }

    "find critical path through graph 2" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          OrientedEdge("1-4", "[1]", "1", "1", "4") :: // 1 --> 4
          OrientedEdge("1-3", "[1]", "1", "1", "3") :: // 1 --> 3
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("2-4", "[2]", "2", "2", "4") :: // 2 --> 4
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          Node("5", "5-20", 20) ::
          Nil
      )

      val expected =
        Node("1", "1-2", 2) ::
          Node("2", "2-3", 3) ::
          Node("3", "3-4", 4) ::
          Node("4", "4-5", 5) ::
          Nil

      GraphOps.findCriticalPathByNodesCount(graph) shouldBe expected
    }

    "find critical path through graph from example" in {
      val graph = OrientedGraph(
        Node("1", "1-5", 5) ::
          Node("2", "2-4", 4) ::
          Node("3", "3-2", 2) ::
          Node("4", "4-20", 20) ::
          Node("5", "5-2", 2) ::
          Node("6", "6-6", 6) ::
          Node("7", "7-1", 1) ::
          Node("8", "8-3", 3) ::
          Node("9", "9-5", 5) ::
          OrientedEdge("1-5", "[2]", "2", "1", "5") :: // 1 --> 5
          OrientedEdge("1-8", "[3]", "3", "1", "8") :: // 1 --> 8
          OrientedEdge("2-5", "[1]", "1", "2", "5") :: // 2 --> 5
          OrientedEdge("2-6", "[2]", "2", "2", "6") :: // 2 --> 6
          OrientedEdge("2-8", "[1]", "1", "2", "8") :: // 2 --> 8
          OrientedEdge("3-7", "[4]", "4", "3", "7") :: // 3 --> 7
          OrientedEdge("5-8", "[3]", "3", "5", "8") :: // 5 --> 8
          OrientedEdge("5-9", "[2]", "2", "5", "9") :: // 5 --> 9
          OrientedEdge("6-9", "[1]", "1", "6", "9") :: // 6 --> 9
          OrientedEdge("7-8", "[4]", "4", "7", "8") :: // 7 --> 8
          OrientedEdge("7-9", "[2]", "2", "7", "9") :: // 7 --> 9
          Nil
      )

      val expected =
        Node("1", "1-5", 5) ::
          Node("5", "5-2", 2) ::
          Node("8", "8-3", 3) ::
          Nil

      val actual = GraphOps.findCriticalPathByNodesCount(graph)
      actual should have size 3
      actual shouldBe expected
    }
  }

  "GraphOps.determineNodeConnectivity" should {
    "work correctly for empty graph" in {
      GraphOps.determineNodeConnectivity(OrientedGraph(List.empty), Node("1", "1-0", 0)) shouldBe 0
    }

    "find node connectivity in trivial case" in {
      val singleNodeGraph = OrientedGraph(Node("1", "node1", 2) :: Nil)
      GraphOps.determineNodeConnectivity(singleNodeGraph, singleNodeGraph.nodes.head) shouldBe 0
    }

    "find node connectivity in simple graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          OrientedEdge("4-5", "[4]", "4", "4", "5") :: // 4 --> 5
          Node("5", "5-6", 6) ::
          OrientedEdge("5-6", "[5]", "5", "5", "6") :: // 5 --> 6
          Node("6", "6-7", 7) ::
          Nil
      )

      GraphOps.determineNodeConnectivity(graph, Node("3", "3-4", 4)) shouldBe 2
    }

    "find node connectivity in graph" in {
      val graph = OrientedGraph(
        Node("1", "1-2", 2) ::
          OrientedEdge("1-2", "[1]", "1", "1", "2") :: // 1 --> 2
          OrientedEdge("1-4", "[1]", "1", "1", "4") :: // 1 --> 4
          OrientedEdge("1-3", "[1]", "1", "1", "3") :: // 1 --> 3
          Node("2", "2-3", 3) ::
          OrientedEdge("2-3", "[2]", "2", "2", "3") :: // 2 --> 3
          OrientedEdge("2-4", "[2]", "2", "2", "4") :: // 2 --> 4
          Node("3", "3-4", 4) ::
          OrientedEdge("3-4", "[3]", "3", "3", "4") :: // 3 --> 4
          Node("4", "4-5", 5) ::
          Nil
      )

      GraphOps.determineNodeConnectivity(graph, Node("3", "3-4", 4)) shouldBe 3
    }
  }
}
