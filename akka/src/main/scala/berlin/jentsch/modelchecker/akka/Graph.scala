package berlin.jentsch.modelchecker.akka

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.mutable.{Graph => XGraph}

import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
  * Directed mutable graph
  */
private[akka] final class Graph[E] private[Graph] (private val wrapped: XGraph[E, DiEdge])
    extends AnyVal {
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

  def withDirectPrecursor(filter: E => Boolean): Set[E] =
    wrapped.nodes
      .filter(node => node.diSuccessors.forall(s => filter(s.toOuter)))
      .map(_.toOuter)
      .to

  private def nodesOf(values: Set[E]): Set[NodeT] =
    wrapped.nodes.filter(node => values(node.value)).to[Set]

  def nodes: Set[E] =
    wrapped.nodes.map(_.value).to[Set]

  override def toString: String = wrapped.toString
}

private[akka] object Graph {
  def apply[E: ClassTag](pairs: (E, E)*): Graph[E] = {
    val edges = pairs.map { case (a, b) => DiEdge(a, b)}
    val xgraph: XGraph[E, DiEdge] = XGraph.from(edges = edges)

    new Graph(xgraph)
  }

  def empty[E: ClassTag]: Graph[E] =
    new Graph[E](XGraph.empty[E, DiEdge])

  def singleton[E: ClassTag](singleton: E): Graph[E] =
    new Graph[E](XGraph.from[E, DiEdge](nodes = singleton :: Nil, edges = Nil))

  def explore[E: ClassTag](
      init: Traversable[E]
  )(successors: E => Traversable[E]): Graph[E] = {

    val xgraph = XGraph.empty[E, DiEdge]

    @tailrec
    def depthFirstSearch(
        unvisited: List[E],
        visited: Set[E],
    ): Unit =
      unvisited match {
        case Nil =>
          // done
        case e :: es if visited(e) =>
          depthFirstSearch(es, visited)
        case e :: es =>
          val visited2 = visited + e
          val succs = successors(e)
          xgraph ++= succs.map { succ =>
            DiEdge(e, succ)
          }
          val unvisited2 = succs.filterNot(visited).to[List] ++ unvisited
          depthFirstSearch(unvisited2, visited2)
      }

    depthFirstSearch(init.to[List], Set.empty)

    new Graph(xgraph)
  }

}
