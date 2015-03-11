package actors.modelchecking

import org.scalatest.prop._
import org.scalatest.{ Matchers, PropSpec }

class ChooseSpec
  extends PropSpec
  with GeneratorDrivenPropertyChecks
  with Matchers
  with Choose {

  property("chooseNext should return false after reset") {
    reset()
    chooseNext() should be(false)
  }

  property("chooseNext should return true after a call of choose") {
    reset()
    choose(1, 2)
    chooseNext() should be(true)
  }

  property("chooseNext should return false after a none choise") {
    reset()
    choose(1)
    chooseNext() should be(false)
  }

  property("the example should be correct") {
    var result = Set.empty[String]
    def println(msg: String) =
      result = result + msg

    reset()
    do {
      if (choose(true, false)) {
        println(choose("1", "2", "3"))
      } else {
        println("Hello " + choose("world", "homer"))
      }
    } while (chooseNext())

    result should be(Set("1", "2", "3", "Hello world", "Hello homer"))
  }

}
