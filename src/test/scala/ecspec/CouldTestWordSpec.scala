package ecspec

import org.scalatest.exceptions.TestFailedException
import org.scalatest.{FlatSpec, Matchers}

class CouldTestWordSpec extends FlatSpec with Matchers with EcSpec {

  "could" should "work on every data type" in {
    true could be(true)
    1 could be(2)
    "" could be(10)
  }

  it should "not confuse multiple expression on one line" in {
    an[TestFailedException] should be thrownBy {
      everyInterleaving { implicit ec =>
        // for this test it's important that this to statements are on one line, don't reformat
        true could be(true); false could be(true)
      }
    }
  }
}
