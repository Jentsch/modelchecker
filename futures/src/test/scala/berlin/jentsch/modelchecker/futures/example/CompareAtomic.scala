package berlin.jentsch.modelchecker.futures.example

import berlin.jentsch.modelchecker.futures.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class CompareAtomic extends FlatSpec with EcSpec with Matchers {

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

  "with uninterrupted" should "even faster" in everyInterleaving { implicit ec =>
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
