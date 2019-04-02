package berlin.jentsch.modelchecker.akka

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.mutable.{Graph => XGraph}

import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
  * Directed mutable graph
  */
final class Graph[E] private[Graph] (private val wrapped: XGraph[E, DiEdge]) extends AnyVal {
  import wrapped._

  def withAncestors(nodes: Set[E], filter: E => Boolean = _ => true): Set[E] = {

    @tailrec
    def withAncestorsRec(
        unvisited: Set[NodeT],
        nodes: Set[NodeT] = Set.empty[NodeT]
    ): Set[NodeT] = {
      if (unvisited.isEmpty)
        nodes
      else {
        val nodes2 = nodes + unvisited.head
        val succs = unvisited.head.diPredecessors.filter(filter)
        val unvisited2 =
          (unvisited.drop(1) ++ succs).filterNot(nodes2)
        withAncestorsRec(unvisited2, nodes2)
      }
    }

    withAncestorsRec(nodesOf(nodes)).map { _.value }

  }

  def directAncestors(nodes: Set[E]): Set[E] =
    nodes.flatMap(directAncestors)

  def directAncestors(node: E): Set[E] =
    get(node).diPredecessors.map(_.value)

  private def nodesOf(values: Set[E]): Set[NodeT] =
    wrapped.nodes.filter(node => values(node.value)).to[Set]

  def nodes: Set[E] =
    wrapped.nodes.map(_.value).to[Set]

  def +=(edge: (E, E)): Unit =
    wrapped.add(DiEdge(edge._1, edge._2))

  override def toString: String = wrapped.toString
}

object Graph {
  def apply[E: ClassTag](pairs: (E, E)*): Graph[E] = {
    val graph = empty[E]
    pairs.foreach(graph += _)

    graph
  }

  def empty[E: ClassTag]: Graph[E] =
    new Graph[E](XGraph.empty[E, DiEdge])

  def singleton[E: ClassTag](singleton: E): Graph[E] =
    new Graph[E](XGraph.from[E, DiEdge](nodes = singleton :: Nil, edges = Nil))

  def explore[E: ClassTag](
      init: Traversable[E]
  )(successors: E => Traversable[E]): Graph[E] = {

    @tailrec
    def depthFirstSearch(
        unvisited: List[E],
        visited: Set[E],
        graph: XGraph[E, DiEdge]
    ): Graph[E] =
      unvisited match {
        case Nil =>
          new Graph(graph)
        case e :: es if visited(e) =>
          depthFirstSearch(es, visited, graph)
        case e :: es =>
          val visited2 = visited + e
          val succs = successors(e)
          val graph2 = graph ++ succs.map { succ =>
            DiEdge(e, succ)
          }
          val unvisited2 = succs.filterNot(visited).to[List] ++ unvisited
          depthFirstSearch(unvisited2, visited2, graph2)
      }

    depthFirstSearch(init.to[List], Set.empty, XGraph.empty)
  }

}
