package com.yevhenii.cluster.planner.server.graphs

import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.cluster.planner.server.model._

object GraphOps extends LazyLogging {

  // todo: useful if we need to find all not connected nodes
//  def checkGraphForConnectivity(entries: List[NonOrientedGraph]): Boolean = {
//    val edges = entries.collect { case e: NonOrientedEdge => e }
//    val nodes = entries.collect { case n: Node => n }.toSet
//
//    def loop(currNode: Node, visitedNodes: Set[Node]): Set[Node] = {
//      val nextVisited = visitedNodes + currNode
//
//      if (nextVisited.size == nodes.size) {
//        nextVisited
//      } else {
//        val nextNodes = nodes.diff(nextVisited).filter(n => isConnected(currNode, n, edges))
//
//        nextNodes.map(n => loop(n, nextVisited)).fold(nextVisited)(_ ++ _)
//      }
//    }
//
//    if (nodes.isEmpty) true
//    else loop(nodes.head, Set.empty).size == nodes.size
//  }

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
    val nodesMap = graph.nodes
      .map(n => (n.id, n))
      .toMap

    def loop(curr: Node, route: Set[Node]): Boolean = {
      val nextNodes = determineNextNodes(curr, graph.edges, nodesMap)

      if (nextNodes.exists(route.contains)) {
        false
      } else {
        nextNodes
          .map(next => loop(next, route + next))
          .forall(identity)
      }
    }

    nodesMap
      .values
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
    if (!checkGraphForCycles(taskGraph) || !taskGraph.nodes.contains(from) || !taskGraph.nodes.contains(target)) {
      None
    } else {
      val nodeMap = taskGraph.nodes.view.map(n => (n.id, n)).toMap
      findPathUnsafe(taskGraph.edges, nodeMap, from, target)
    }
  }

  private def findPathUnsafe(
    edges: List[OrientedEdge],
    nodeMap: Map[String, Node],
    from: Node,
    target: Node
  ): Option[Set[List[Node]]] = {

    def loop(path: List[Node], curr: Node): Option[Set[List[Node]]] = {
      val currPath = path :+ curr

      if (curr == target) {
        Some(Set(currPath))
      } else {
        val nextVertices = determineNextNodes(curr, edges, nodeMap)

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
      val initialNodes = findInitialVertices(taskGraph)
      val terminalNodes = findTerminalVertices(taskGraph)

      val nodesMap = taskGraph.nodes.view.map(n => (n.id, n)).toMap

      val allPaths: Seq[Set[List[Node]]] = for {
        initial <- initialNodes
        terminal <- terminalNodes
        paths <- findPathUnsafe(taskGraph.edges, nodesMap, initial, terminal)
      } yield paths

      val flatPaths = allPaths.flatten

      flatPaths.maxBy(path => path.map(node => node.weight).sum)
    }
  }

  def findCriticalPathLength(taskGraph: OrientedGraph): Int =
    findCriticalPath(taskGraph)
      .map(node => node.weight)
      .sum

  private def isConnected(first: Node, second: Node, edges: List[NonOrientedEdge]): Boolean =
    edges.exists(edge => (edge.source == first.id) && (edge.target == second.id)) ||
      edges.exists(edge => (edge.target == first.id) && (edge.source == second.id))

  private def isConnectedOriented(first: Node, second: Node, edges: List[OrientedEdge]): Boolean =
    edges.exists(edge => (edge.source == first.id) && (edge.target == second.id))

  private def determineNextNodes(node: Node, edges: List[OrientedEdge], nodes: Map[String, Node]): List[Node] =
    edges.filter(_.source == node.id)
      .map(_.target)
      .flatMap(nodes.get)
}
