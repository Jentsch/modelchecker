package ecspec

import org.specs2._

object `Traverser Specification` extends Specification {
  def is =
    s2"""

The traverser offers after initialisation two methods:

- `choose` requires a not empty sequence and return one element 'randomly'
- `hasMoreOptions` returns true if an other run of the code block reveals new options

In the example
```scala
val traverser = new Traverser

var visit = Set.empty[String]

do {
  val first = traverser.choose(Seq(1, 2))

  if (traverser.choose(Seq(true, false)))
    visit += first.toString
  else
    visit += (first.toString + traverser.choose(Seq("A", "B")))
} while (traverser.hasMoreOptions)
```

   should see all possible outcomes $allPossibleOutcomes
   (1, 2, 1A, 1B, 2A, 2B)
"""

  def allPossibleOutcomes = {
    val traverser = new Traverser

    var visit = Set.empty[String]

    do {
      val first = traverser.choose(Seq(1, 2))

      if (traverser.choose(Seq(true, false)))
        visit = visit + first.toString
      else
        visit = visit + (first.toString + traverser.choose(Seq("A", "B")))
    } while (traverser.hasMoreOptions)

    visit must beEqualTo(Set("1", "2", "1A", "1B", "2A", "2B"))
  }

}