package berlin.jentsch.modelchecker.akka.example

import akka.actor.ActorPath
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

  private def signal: ActorPath = root

  red should "get yellow and than green" in (
    signal is red,
    (signal is yellow).isInevitable,
  )

}
