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

  def checkGraphForConnectivity(entries: List[NonOrientedGraph]): Boolean = {
    val edges = entries.collect { case e: NonOrientedEdge => e }
    val nodes = entries.collect { case n: Node => n }.toSet

    def loop(currNode: Node, unvisitedNodes: Set[Node]): Boolean = {
      if (unvisitedNodes.isEmpty) {
        true
      } else {
        val nextNodes = unvisitedNodes.filter(n => isConnected(n, currNode, edges))

        val nextUnvisited = unvisitedNodes -- nextNodes

        nextNodes.map(n => loop(n, nextUnvisited)).fold(false)(_ || _)
      }
    }

    if (nodes.isEmpty) true
    else loop(nodes.head, nodes.tail)
  }

  def checkGraphForCycles(entries: List[OrientedGraph]): Boolean = {
    val edges = entries.collect { case e: OrientedEdge => e }

    val nodesMap = entries.view
      .collect { case n: Node => n }
      .map(n => (n.id, n))
      .toMap

    def loop(curr: Node, route: Set[Node]): Boolean = {
      val nextNodes = determineNextNodes(curr, edges, nodesMap)

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

  def findInitialVertices(taskGraph: List[OrientedGraph]): List[Node] = {
    val edges = taskGraph.collect { case e: OrientedEdge => e }
    val nodes = taskGraph.collect { case n: Node => n }

    nodes.filterNot(node => edges.exists(e => e.target == node.id))
  }

  def findTerminalVertices(taskGraph: List[OrientedGraph]): List[Node] = {
    val edges = taskGraph.collect { case e: OrientedEdge => e }
    val nodes = taskGraph.collect { case n: Node => n }

    nodes.filterNot(node => edges.exists(e => e.source == node.id))
  }

  // TODO
//  def findPaths(taskGraph: List[OrientedGraph], from: Node, to: Node): Set[List[Node]] = {
//    val edges = taskGraph.collect { case e: OrientedEdge => e }
//    val nodes = taskGraph.collect { case n: Node => n }
//
//    Set()
//  }

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
