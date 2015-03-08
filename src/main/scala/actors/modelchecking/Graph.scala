package actors.modelchecking

import scala.annotation.tailrec
import scala.reflect.runtime.universe._
import scalax.collection.Graph._
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.{ Graph => XGraph }

/**
 * Directed graph
 */
final class Graph[E] private[Graph] (private val wrapped: XGraph[E, DiEdge]) {
  import wrapped._

  def withAncestors(nodes: Set[E]): Set[E] =
    withAncestorsRec(nodesOf(nodes)).map { _.value }

  //TODO: create a generic depth first search for `withAncestorsRec` and `explore`
  private def withAncestorsRec(unvisited: Set[NodeT], nodes: Set[NodeT] = Set.empty[NodeT]): Set[NodeT] = {
    if (unvisited.isEmpty)
      nodes
    else {
      val nodes2 = nodes + unvisited.head
      val unvisited2 =
        (unvisited.head.diPredecessors ++ unvisited.drop(1)).
          filterNot(nodes2)
      withAncestorsRec(unvisited2, nodes2)
    }
  }

  private def nodesOf(values: Set[E]): Set[NodeT] =
    wrapped.nodes.filter(node => values(node.value)).to[Set]

  def nodes: Set[E] =
    wrapped.nodes.map(_.value).to[Set]

  def ++(edges: Iterable[(E, E)]): Graph[E] =
    new Graph(wrapped ++ edges.map { case (from, to) => DiEdge(from, to) })

  override def toString = wrapped.toString

  override def equals(any: Any) = any match {
    case that: Graph[_] => this.wrapped == that.wrapped
    case _              => false
  }

  override def hashCode = wrapped.hashCode
}

object Graph {
  def empty[E: TypeTag] =
    new Graph[E](XGraph.empty[E, DiEdge])

  def apply[E: TypeTag](pairs: (E, E)*): Graph[E] =
    empty ++ pairs

  def explore[E: TypeTag](init: Traversable[E])(successors: E => Traversable[E]): Graph[E] = {

    def depthFirstSearch(unvisited: List[E], visited: Set[E], graph: XGraph[E, DiEdge]): Graph[E] =
      unvisited match {
        case Nil =>
          new Graph(graph)
        case e :: es if visited(e) =>
          depthFirstSearch(es, visited, graph)
        case e :: es =>
          val visited2 = visited + e
          val succs = successors(e)
          val graph2 = graph ++ succs.map { succ => DiEdge(e, succ) }
          val unvisited2 = succs.filterNot(visited).to[List] ++ unvisited
          depthFirstSearch(unvisited2, visited2, graph2)
      }

    depthFirstSearch(init.to[List], Set.empty, XGraph.empty)
  }

}
