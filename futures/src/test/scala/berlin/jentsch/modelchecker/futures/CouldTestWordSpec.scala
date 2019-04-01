package berlin.jentsch.modelchecker.futures

import org.scalatest.{FlatSpec, Matchers}

class CouldTestWordSpec extends FlatSpec with Matchers with EcSpec {
  behavior of "Could word"

  it should "not capture statements outside the interleaving context" in pendingUntilFixed {
    true could be(false)

    everyInterleaving { _ =>
      1 could be(1)
    }
  }
}
