package berlin.jentsch.modelchecker.akka

import org.scalatest.{Matchers, PropSpec}

class GraphExplorerSpec extends PropSpec with Matchers {

  property("explore") {
    val one = GraphExplorer.explore(Set(1))(i => Set(i))
    one should be(GraphExplorer(1 -> 1))

    val two = GraphExplorer.explore(Set(1, 2, -2))(i => Set(-i))
    two should be(GraphExplorer(1 -> -1, -1 -> 1, 2 -> -2, -2 -> 2))

    val subSets = GraphExplorer.explore(Set(Set(1, 2, 3))) { set =>
      set.map(set - _)
    }
    subSets should be(
      GraphExplorer(
        Set(1, 2, 3) -> Set(1, 2),
        Set(1, 2, 3) -> Set(1, 3),
        Set(1, 2, 3) -> Set(2, 3),
        Set(1, 2) -> Set(1),
        Set(1, 2) -> Set(2),
        Set(1, 3) -> Set(1),
        Set(1, 3) -> Set(3),
        Set(2, 3) -> Set(2),
        Set(2, 3) -> Set(3),
        Set(1) -> Set(),
        Set(2) -> Set(),
        Set(3) -> Set()
      )
    )
  }

  property("toString") {
    GraphExplorer().toString shouldBe "Graph()"
  }

}
