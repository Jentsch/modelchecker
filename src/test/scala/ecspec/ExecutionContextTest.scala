package ecspec

import org.scalatest.{Matchers, PropSpec}

import scala.concurrent.Future

class ExecutionContextTest extends PropSpec with Matchers with EcSpec {

  property("find all possible solutions") {
    everyPossiblePath { implicit ec =>
      var x = 0

      atomic {
        Future {
          x = 1
        }
        Future {
          x = 2
        }
      }

      x should (be(0) or be(1) or be(2))

      x could be(0)
      x could be(1)
      x could be(2)

      // Would fail:
      // x could be > (10)
    }
  }

}
