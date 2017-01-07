package actors.modelchecking

import org.scalatest.{Matchers, PropSpec}

class GraphSpec extends PropSpec with Matchers {

  property("Empty graphs") {
    Graph.empty.nodes shouldBe empty
  }

  property("Create graphs") {
    val g = Graph(1 -> 2, 2 -> 3, 3 -> 1, 3 -> 4)
    g.nodes should be(Set(1, 2, 3, 4))
  }

  property("all ancestors") {
    val g = Graph(1 -> 2, 2 -> 3, 3 -> 1, 3 -> 4, 3 -> 5, 5 -> 5)

    g withAncestors Set(4) should be(Set(1, 2, 3, 4))
    g withAncestors Set(5) should be(Set(1, 2, 3, 5))
    g withAncestors Set(1) should be(Set(1, 2, 3))
    g withAncestors Set(1, 2) should be(Set(1, 2, 3))
  }

  property("explore") {
    val one = Graph.explore(Set(1))(i => Set(i))
    one should be(Graph(1 -> 1))

    val two = Graph.explore(Set(1, 2, -2))(i => Set(-i))
    two should be(Graph(1 -> -1, -1 -> 1, 2 -> -2, -2 -> 2))

    val subSets = Graph.explore(Set(Set(1, 2, 3))) { set =>
      set.map(set - _)
    }
    subSets should be(
      Graph(
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
      ))
  }

}
