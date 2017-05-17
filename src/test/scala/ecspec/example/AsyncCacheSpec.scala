package ecspec.example

import ecspec.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.util.{Failure, Success, Try}

class AsyncCacheSpec extends FlatSpec with EcSpec with Matchers {

  "Async Cache" should "return same result as given function" in everyInterleaving {
    implicit ec =>
      val f = Map(1 -> successful("1"))
      val cache = AsyncCache(f)

      ec.execute { () =>
        cache(1) will be("1")
      }
      ec.execute { () =>
        cache(1) will be("1")
      }
  }

  it should "not invoke the function twice for same argument" in everyInterleaving {
    implicit ec =>
      val observedRequests = ListBuffer.empty[Int]
      val f = { i: Int =>
        Future {
          observedRequests += i
          i.toString
        }
      }
      val cache = AsyncCache(f)

      ec.execute { () =>
        cache(1)
      }
      ec.execute { () =>
        cache(1)
      }

      observedRequests.diff(List(1, 2)) shouldBe empty
  }

  it should "not store failures" in everyInterleaving { implicit ec =>
    var response: Try[String] = Failure[String](new RuntimeException(""))
    val f = { _: Int =>
      Future { response.get }
    }
    val cache = AsyncCache(f)

    cache(1).onComplete { t =>
      t should be a 'failure

      response = Success("1")

      cache(1) will be("1")
    }
  }
}
