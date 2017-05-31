package ecspec.example

import ecspec.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AsyncCacheSpec extends FlatSpec with EcSpec with Matchers {

  behavior of "Async Cache"

  it should "return same result" in everyInterleaving { implicit ec =>
    val f = { i: Int =>
      Future { i.toString }
    }
    val cache = AsyncCache(f)

    cache(1) will be("1")
    cache(1) will be("1")
  }

  it should "cache" in everyInterleaving { implicit ec =>
    var seenRequest = Set.empty[Int]

    val f = { i: Int =>
      seenRequest should not contain i
      seenRequest += i
      Future {
        i.toString
      }
    }
    val cache = AsyncCache(f)

    atomic {
      ec.execute(() => cache(1) will be("1"))
      ec.execute(() => cache(1) will be("1"))
    }
  }

  it should "not store failures" in everyInterleaving {
    implicit ec =>
      var response: Try[String] =
        Failure(new FindException)

      val f = { _: Int =>
        Future { response.get }
      }

      val cache = AsyncCache(f)

      atomic {
        ec.execute(() => cache(1).onComplete { response =>
          response could be(a[Failure[_]])
          response could be(a[Success[_]])
        })

        ec.execute(() => cache(1).onComplete { r =>
          r should be a 'failure
          response = Success("What ever")

          cache(1) will be("What ever")
        })

      }
  }
}

class FindException(val x: Int) extends RuntimeException("FindExeption: " + x.toString)
