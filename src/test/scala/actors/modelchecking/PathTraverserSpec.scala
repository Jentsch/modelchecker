package actors.modelchecking

import org.scalatest.{Matchers, PropSpec}

class PathTraverserSpec extends PropSpec with Matchers {

  val testUnderTest = new PathTraverser
  import testUnderTest._

  property("chooseNext should return false after reset") {
    reset()
    chooseNext() should be(false)
  }

  property("chooseNext should return true after a call of choose") {
    reset()
    choose(1 :: 2 :: Nil)
    chooseNext() should be(true)
  }

  property("chooseNext should return false after a none choice") {
    reset()
    choose(1 :: Nil)
    chooseNext() should be(false)
  }

  property("the example should be correct") {
    var result = Set.empty[String]
    def println(msg: Any) =
      result = result + msg.toString

    reset()
    do {
      if (choose(true :: false :: Nil)) {
        println(choose(1 :: 2 :: 3 :: Nil))
      } else {
        println("Hello " + choose("world" :: "homer" :: Nil))
      }
    } while (chooseNext())

    result should be(Set("1", "2", "3", "Hello world", "Hello homer"))
  }

}
