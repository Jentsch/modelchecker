package berlin.jentsch.modelchecker.futures

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class CouldTestWordSpec extends AnyFlatSpec with Matchers with EcSpec {
  behavior of "Could word"

  it should "not capture statements outside the interleaving context" in pendingUntilFixed {
    true could be(false)

    everyInterleaving { _ =>
      1 could be(1)
    }
  }
}
