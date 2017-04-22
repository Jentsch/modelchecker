package ecspec

import org.scalatest.{FlatSpec, Matchers}

class CouldTestWordSpec extends FlatSpec with Matchers with EcSpec {

  "could" should "work on every data type" in {
    true could be(true)
    1 could be(2)
    "" could be(10)
  }
}
