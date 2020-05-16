package com.yevhenii.cluster.planner.server.graphs

import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model._

object GraphOps extends LazyLogging {

  def checkGraphForConnectivity(graph: NonOrientedGraph): Boolean = {
    val nodes = graph.nodes.toSet

    def loop(currNode: Node, unvisitedNodes: Set[Node]): Boolean = {
      if (unvisitedNodes.isEmpty) {
        true
      } else {
        val nextNodes = unvisitedNodes.filter(n => isConnected(n, currNode, graph.edges))

        val nextUnvisited = unvisitedNodes -- nextNodes

        nextNodes.map(n => loop(n, nextUnvisited)).fold(false)(_ || _)
      }
    }

    if (nodes.isEmpty) true
    else loop(nodes.head, nodes.tail)
  }

  def checkGraphForCycles(graph: OrientedGraph): Boolean = {
    def loop(curr: Node, route: Set[Node]): Boolean = {
      val nextNodes = determineNextNodes(graph, curr)

      if (nextNodes.exists(route.contains)) {
        false
      } else {
        nextNodes
          .map(next => loop(next, route + next))
          .forall(identity)
      }
    }

    graph.nodes
      .map(n => loop(n, Set(n)))
      .forall(identity)
  }

  def findInitialVertices(taskGraph: OrientedGraph): List[Node] = {
    taskGraph.nodes
      .filterNot(node => taskGraph.edges.exists(e => e.target == node.id))
  }

  def findTerminalVertices(taskGraph: OrientedGraph): List[Node] = {
    taskGraph.nodes
      .filterNot(node => taskGraph.edges.exists(e => e.source == node.id))
  }

  def findPaths(taskGraph: OrientedGraph, from: Node, target: Node): Option[Set[List[Node]]] = {
    if (!taskGraph.isCorrect || !taskGraph.nodes.contains(from) || !taskGraph.nodes.contains(target)) {
      None
    } else {
      findPathUnsafe(taskGraph, from, target)
    }
  }

  private def findPathUnsafe(graph: OrientedGraph, from: Node, target: Node): Option[Set[List[Node]]] = {
    def loop(path: List[Node], curr: Node): Option[Set[List[Node]]] = {
      val currPath = path :+ curr

      if (curr == target) {
        Some(Set(currPath))
      } else {
        val nextVertices = determineNextNodes(graph, curr)

        nextVertices.map(next => loop(currPath, next))
          .fold(None) { (first, second) =>
            (first, second) match {
              case (Some(f), Some(s)) => Some(f ++ s)
              case (None, None) => None
              case (someFirst, None) => someFirst
              case (None, someSecond) => someSecond
            }
          }
      }
    }

    loop(Nil, from)
  }

  def findCriticalPath(taskGraph: OrientedGraph): List[Node] = {
    if (taskGraph.nodes.isEmpty || !checkGraphForCycles(taskGraph)) {
      Nil
    } else {
      taskGraph.initialVertices
        .flatMap(node => findCriticalPath(taskGraph, node))
        .maxBy(path => path.map(_.weight).sum)
    }
  }

  def findCriticalPathByNodesCount(taskGraph: OrientedGraph): List[Node] = {
    if (taskGraph.nodes.isEmpty || !taskGraph.isCorrect) {
      Nil
    } else {
      taskGraph.initialVertices.view
        .flatMap(node => findCriticalPathByNodesCount(taskGraph, node))
        .maxBy(path => path.length)
    }
  }

  def findCriticalPathLength(taskGraph: OrientedGraph): Int =
    findCriticalPath(taskGraph)
      .map(node => node.weight)
      .sum

  def findCriticalPath(taskGraph: OrientedGraph, from: Node): Option[List[Node]] = {
    if (taskGraph.nodes.isEmpty || !taskGraph.isCorrect) {
      None
    } else {
      val allPaths = taskGraph.terminalVertices
        .flatMap(terminal => findPathUnsafe(taskGraph, from, terminal))
        .flatten

      if (allPaths.isEmpty) None
      else Some(allPaths.maxBy(path => path.map(node => node.weight).sum))
    }
  }

  def findCriticalPathByNodesCount(taskGraph: OrientedGraph, from: Node): Option[List[Node]] = {
    if (taskGraph.nodes.isEmpty || !taskGraph.isCorrect) {
      None
    } else {
      val allPaths = taskGraph.terminalVertices.view
        .flatMap(terminal => findPathUnsafe(taskGraph, from, terminal))
        .flatten

      if (allPaths.isEmpty) None
      else Some(allPaths.maxBy(path => path.length))
    }
  }

  def determineNodeConnectivity[E <: GraphEntry](taskGraph: Graph[E], node: Node): Int = {
    taskGraph.edges.count(edge => edge.target == node.id || edge.source == node.id)
  }

  def correlationOfConnections(taskGraph: OrientedGraph): Double = {
    correlationOfConnections(taskGraph.nodes, taskGraph.edges)
  }

  def correlationOfConnections(nodes: List[Node], edges: List[OrientedEdge]): Double = {
    val nodeWeights = nodes.map(_.weight).sum
    val edgeWeights = edges.map(_.weight).sum

    correlationOfConnections(nodeWeights, edgeWeights)
  }

  def correlationOfConnections(nodeWeights: Int, edgeWeights: Int): Double = {
    val allWeights = nodeWeights + edgeWeights

    if (allWeights == 0) 0
    else nodeWeights.toDouble / allWeights
  }

  def findShortestPath(from: String, to: String, sendingAmount: Int, nonOrientedGraph: NonOrientedGraph): List[NonOrientedEdge] = {
    val edges = nonOrientedGraph.edges
    def loop(currNode: String, path: List[NonOrientedEdge]): List[List[NonOrientedEdge]] = {
      if (currNode == to) {
        List(path)
      } else {
        val next = edges.filter(edge => edge.source == currNode || edge.target == currNode)

        next.filterNot(path.contains)
          .flatMap { edge =>
            val nextNode = if (edge.source == currNode) edge.target else edge.source
            loop(nextNode, edge :: path)
          }
          .filterNot(_.isEmpty)
      }
    }

    loop(from, List.empty)
      .filterNot(_.isEmpty)
      .sortBy(_.map(e => math.ceil(sendingAmount / e.weight).toInt).sum)(Ordering.Int)
      .head
  }

  def findShortestPathInNodes(from: String, to: String, sendingAmount: Int, nonOrientedGraph: NonOrientedGraph): List[String] = {
    val edges = nonOrientedGraph.edges
    def loop(currNode: String, path: List[NonOrientedEdge], nodes: List[String]): List[(List[NonOrientedEdge], List[String])] = {
      if (currNode == to) {
        List((path, nodes.reverse))
      } else {
        val next = edges.filter(edge => edge.source == currNode || edge.target == currNode)

        next.filterNot(path.contains)
          .flatMap { edge =>
            val nextNode = if (edge.source == currNode) edge.target else edge.source
            loop(nextNode, edge :: path, nextNode :: nodes)
          }
          .filterNot(_._1.isEmpty)
      }
    }

    loop(from, List.empty, List(from))
      .filterNot(_._1.isEmpty)
      .sortBy(_._1.map(e => math.ceil(sendingAmount / e.weight).toInt).sum)(Ordering.Int)
      .head
      ._2
  }

  private def isConnected[E <: NonOrientedGraphEntry with Edge](first: Node, second: Node, edges: List[E]): Boolean =
    edges.exists(edge => (edge.source == first.id) && (edge.target == second.id)) ||
      edges.exists(edge => (edge.target == first.id) && (edge.source == second.id))

  private def isConnectedOriented(first: Node, second: Node, edges: List[OrientedEdge]): Boolean =
    edges.exists(edge => (edge.source == first.id) && (edge.target == second.id))

  private def determineNextNodes(graph: OrientedGraph, node: Node): List[Node] =
    graph.edges.filter(_.source == node.id)
      .map(_.target)
      .flatMap(graph.nodesMap.get)

  object Implicits {
    implicit class OrientedGraphOpsImplicits(orientedGraph: OrientedGraph) {
      def findPaths(from: Node, to: Node): Option[Set[List[Node]]] = GraphOps.findPaths(orientedGraph, from, to)
      def criticalPath: List[Node] = GraphOps.findCriticalPath(orientedGraph)
      def criticalPath(node: Node): Option[List[Node]] = GraphOps.findCriticalPath(orientedGraph, node)

      def criticalPathByNodesCount: List[Node] = GraphOps.findCriticalPathByNodesCount(orientedGraph)
      def criticalPathByNodesCount(node: Node): Option[List[Node]] = GraphOps.findCriticalPathByNodesCount(orientedGraph, node)

      def connectivityOfNode(node: Node): Int = GraphOps.determineNodeConnectivity(orientedGraph, node)
      def correlationOfConnections: Double = GraphOps.correlationOfConnections(orientedGraph)
    }

    implicit class NonOrientedGraphOpsImplicits(nonOrientedGraph: NonOrientedGraph) {
      def connectivityOfNode(node: Node): Int = GraphOps.determineNodeConnectivity(nonOrientedGraph, node)
      def findShortestPath(from: String, to: String, sendingAmount: Int): List[NonOrientedEdge] =
        GraphOps.findShortestPath(from, to, sendingAmount, nonOrientedGraph)
      def findShortestPathInNodes(from: String, to: String, sendingAmount: Int): List[String] =
        GraphOps.findShortestPathInNodes(from, to, sendingAmount, nonOrientedGraph)
    }
  }
}
