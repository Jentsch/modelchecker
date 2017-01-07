package ecspec

import org.specs2._

import scala.collection.mutable

object `Traverser Specification` extends Specification {
  def is =
    s2"""

The traverser offers after initialisation two main methods:

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

## removeOne

When working with mutable collections the `removeOne` method
take on out of the given sequence and returns it.

In the example
```
val traverser = new Traverser

var visit = Set.empty[String]

do {
  val buffer = collection.mutable.Buffer(1, 2)

  println(traverser.removeOne(buffer)
  println(traverser.removeOne(buffer)
} while (traverser.hasMoreOptions)
```

the output is: 1, 2 and 2, 1 $printAllOutcomes
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

  def printAllOutcomes = {
    val traverser = new Traverser

    var visit = Set.empty[(Int, Int)]

    do {
      val buffer = mutable.Buffer(1, 2)

      visit += ((traverser.removeOne(buffer), traverser.removeOne(buffer)))
    } while (traverser.hasMoreOptions)

    visit must beEqualTo(Set((1, 2), (2, 1)))
  }

}
