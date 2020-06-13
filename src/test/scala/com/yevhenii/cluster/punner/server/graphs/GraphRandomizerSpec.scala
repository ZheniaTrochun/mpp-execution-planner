package com.yevhenii.cluster.punner.server.graphs

import cats.Show
import com.yevhenii.cluster.planner.server.graphs.GraphShow._
import com.yevhenii.cluster.planner.server.graphs.{GraphOps, GraphRandomizer}
import com.yevhenii.cluster.planner.server.model.{GraphParameters, OrientedGraph}
import org.scalatest.{Matchers, WordSpec}

class GraphRandomizerSpec extends WordSpec with Matchers {

  "GraphRandomizer.randomOrientedGraph" should {

    val defaultParams = GraphParameters(numberOfNodes = 10, correlation = 0.5)

    def createDefaultGraphs(): Seq[OrientedGraph] =
      for (_ <- 1 to 10) yield GraphRandomizer.randomOrientedGraph(defaultParams)

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

    "create graph with correct edge weights" ignore {
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
        .forall(correlation => correlation == defaultParams.correlation) shouldBe true
    }

    "create graph without edges if desired correlation is 1" in {
      val params = defaultParams.copy(correlation = 1)

      val graphs = for (_ <- 1 to 10)
        yield GraphRandomizer.randomOrientedGraph(params)

      graphs.flatMap(_.edges) shouldBe empty
    }

    "create graph without edges if desired correlation is 0.1" in {
      val params = defaultParams.copy(correlation = 0.1)

      val graphs = for (_ <- 1 to 10)
        yield GraphRandomizer.randomOrientedGraph(params)

      graphs
        .map(GraphOps.correlationOfConnections)
        .forall(correlation => correlation == params.correlation) shouldBe true
    }

    "create graph without edges if desired correlation is 1 and number of nodes is relatively big" in {
      val graphs = GraphRandomizer.randomOrientedGraph(defaultParams.copy(correlation = 1, numberOfNodes = 32))

      graphs.edges shouldBe empty
    }

    "create pretty big graph with correlation 0.1" in {
      val graphs = GraphRandomizer.randomOrientedGraph(defaultParams.copy(correlation = 0.1, numberOfNodes = 32))

      graphs.edges should not be empty
    }

    "just print created graph" ignore {
      val graphs = createDefaultGraphs()

      val sout = graphs.map(Show[OrientedGraph].show).mkString("\n\n")

      println(sout)
    }
  }
}
