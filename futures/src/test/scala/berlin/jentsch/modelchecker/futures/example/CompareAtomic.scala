package berlin.jentsch.modelchecker.futures.example

import berlin.jentsch.modelchecker.futures.EcSpec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class CompareAtomic extends AnyFlatSpec with EcSpec with Matchers {

  "without atomic" should "be slower" in everyInterleaving { implicit ec =>
    var x = 1
    Future { x = 2 }
    Future { x = 3 }
  }

  "with atomic" should "be faster" in everyInterleaving { implicit ec =>
    var x = 1
    atomic {
      Future {
        x = 2
      }
      Future {
        x = 3
      }
    }
  }

  "with uninterrupted" should "even faster" in everyInterleaving {
    implicit ec =>
      var x = 1
      uninterrupted {
        Future {
          x = 2
        }
        Future {
          x = 3
        }
      }
  }

}
