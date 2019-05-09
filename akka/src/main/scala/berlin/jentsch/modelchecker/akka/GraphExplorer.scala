package berlin.jentsch.modelchecker.akka

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.config.CoreConfig
import scalax.collection.immutable.Graph

import scala.collection.mutable
import scala.reflect.ClassTag

private[akka] object GraphExplorer {
  private val maxNodes = 2000

  def explore[E: ClassTag](
      init: Traversable[E]
  )(successors: E => Traversable[E])(
      implicit classTag: ClassTag[DiEdge[E]]
  ): Graph[E, DiEdge] = {

    implicit val config: CoreConfig = Graph.defaultConfig

    val builder = Graph.newBuilder[E, DiEdge]
    val unvisited: mutable.Queue[E] = init.to[mutable.Queue]
    val visited: mutable.Set[E] = mutable.Set.empty

    while (unvisited.nonEmpty) {
      val e = unvisited.dequeue()
      if (!visited(e)) {
        visited += e

        val succs = successors(e)
        builder ++= succs.view.map(DiEdge(e, _))

        if (visited.size >= maxNodes) {
          throw new OutOfMemoryError(s"More than $maxNodes nodes found")
        }
        unvisited ++= succs.view.filterNot(visited)
      }
    }

    builder.result()
  }
}
