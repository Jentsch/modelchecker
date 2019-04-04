package berlin.jentsch.modelchecker.akka.example

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.setup
import berlin.jentsch.modelchecker.akka.AkkaSpec
import berlin.jentsch.modelchecker.akka.example.SimpleSignals._

object SimpleSignals {

  def red: Behavior[Nothing] = setup[Nothing] { _ =>
    yellow
  }

  def yellow: Behavior[Nothing] = setup[Nothing] { _ =>
    green
  }

  def green: Behavior[Nothing] = setup[Nothing] { _ =>
    red
  }
}

class SimpleSignalsSpec extends AkkaSpec {

  behavior of "a simple signal"

  red should "get yellow and than green" in (
    root is red,
    alwaysNext(root is yellow),
    alwaysNext(alwaysNext(root is green)),
    alwaysNext(alwaysNext(alwaysNext(root is red)))
  )

}
