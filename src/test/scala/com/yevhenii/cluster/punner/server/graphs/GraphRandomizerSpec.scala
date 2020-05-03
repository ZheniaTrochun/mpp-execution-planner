package com.yevhenii.cluster.punner.server.graphs

import cats.Show
import com.yevhenii.cluster.planner.server.graphs.GraphShow._
import com.yevhenii.cluster.planner.server.graphs.{GraphOps, GraphRandomizer}
import com.yevhenii.cluster.planner.server.model.{GraphParameters, OrientedGraph}
import org.scalatest.{Matchers, WordSpec}

class GraphRandomizerSpec extends WordSpec with Matchers {

  "GraphRandomizer.createRandomOrientedGraph" should {

    val defaultParams = GraphParameters(numberOfNodes = 10, connectivity = 0.5)

    def createDefaultGraphs(): Seq[OrientedGraph] =
      for (_ <- 1 to 10) yield GraphRandomizer.createRandomOrientedGraph(defaultParams)

    "always create acyclic graph" in {
      val graphs = createDefaultGraphs()

      graphs.forall(GraphOps.checkGraphForCycles) shouldBe true
    }

    "create graph with correct number of nodes" in {
      val graphs = createDefaultGraphs()

      graphs.forall(graph => graph.nodes.size == defaultParams.numberOfNodes) shouldBe true
    }

    "create graph with correct node weights" in {
      val graphs = createDefaultGraphs()

      val min = defaultParams.minimalNodeWeight
      val max = defaultParams.maximumNodeWeight
      
      graphs
        .flatMap(graph => graph.nodes.map(_.weight))
        .forall(weight => weight >= min && weight <= max) shouldBe true
    }

    "create graph with correct edge weights" in {
      val graphs = createDefaultGraphs()

      val min = defaultParams.minimalEdgeWeight
      val max = defaultParams.maximumEdgeWeight

      graphs
        .flatMap(graph => graph.edges.map(_.weight))
        .forall(weight => weight >= min && weight <= max) shouldBe true
    }

    "create graph with correct correlation of node and edge weights" in {
      val graphs = createDefaultGraphs()

      graphs
        .map(GraphOps.correlationOfConnections)
        .forall(correlation => correlation == defaultParams.connectivity) shouldBe true
    }

    "just print created graph" in {
      val graphs = createDefaultGraphs()

      val sout = graphs.map(Show[OrientedGraph].show).mkString("\n\n")

      println(sout)
    }
  }
}
