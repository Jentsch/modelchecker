package berlin.jentsch.modelchecker.akka

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.mutable.{Graph => XGraph}

import scala.collection.mutable
import scala.reflect.ClassTag

private[akka] object GraphExplorer {
  def apply[E: ClassTag](pairs: (E, E)*): Graph[E, DiEdge] =
    XGraph.from(edges = pairs.map { case (a, b) => DiEdge(a, b) })

  def explore[E: ClassTag](
      init: Traversable[E]
  )(successors: E => Traversable[E])(
      implicit classTag: ClassTag[DiEdge[E]]
  ): scalax.collection.Graph[E, DiEdge] = {

    val xgraph = XGraph.empty[E, DiEdge]
    val unvisited: mutable.Queue[E] = init.to[mutable.Queue]
    val visited: mutable.Set[E] = mutable.Set.empty

    while (unvisited.nonEmpty) {
      val e = unvisited.dequeue()
      if (!visited(e)) {
        visited += e

        val succs = successors(e)
        xgraph ++= succs.view.map(DiEdge(e, _))

        val n = 2000
        if (xgraph.nodes.size >= n) {
          // println(xgraph.toString)
          throw new OutOfMemoryError(s"More than $n nodes found")
        }
        unvisited ++= succs.view.filterNot(visited)
      }
    }

    xgraph
  }

}
