package berlin.jentsch.modelchecker.akka.example

import akka.Done
import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors._
import berlin.jentsch.modelchecker.akka._

object Philosophers {
  def apply(): Behavior[Unit] = setup { ctx =>
    val stick1 = ctx.spawn(stick, "Stick1")
    val stick2 = ctx.spawn(stick, "Stick2")
    val stick3 = ctx.spawn(stick, "Stick3")

    ctx.spawn(philosophers(stick1, stick2), "Philosopher1")
    ctx.spawn(philosophers(stick2, stick3), "Philosopher2")
    ctx.spawn(philosophers(stick1, stick3), "Philosopher3")

    empty
  }
  def deadlock: Behavior[Unit] = setup { ctx =>
    val stick1 = ctx.spawn(stick, "Stick1")
    val stick2 = ctx.spawn(stick, "Stick2")
    val stick3 = ctx.spawn(stick, "Stick3")

    ctx.spawn(philosophers(stick1, stick2), "Philosopher1")
    ctx.spawn(philosophers(stick2, stick3), "Philosopher2")
    ctx.spawn(philosophers(stick3, stick1), "Philosopher3")

    empty
  }

  sealed trait Messages
  case class Req(sender: ActorRef[Done]) extends Messages
  case object Free extends Messages

  def stick: Behavior[Messages] = stickFree

  lazy val stickFree: Behavior[Messages] = receiveMessagePartial {
    case Req(sender) =>
      sender ! Done
      stickInUse
  }

  lazy val stickInUse: Behavior[Messages] = receiveMessage {
    case Free        => stickFree
    case Req(sender) => receiveMessagePartial {
      case Free =>
        sender ! Done
        stickInUse
    }
  }

  def philosophers(
      stick1: ActorRef[Messages],
      stick2: ActorRef[Messages]
  ): Behavior[Done] = {

    def acquireFirstStick: Behavior[Done] = setup { ctx =>
      stick1 ! Req(ctx.self)
      receiveMessage { case Done => acquireSecondStick }
    }

    def acquireSecondStick: Behavior[Done] = setup { ctx =>
      stick2 ! Req(ctx.self)
      receiveMessage { case Done => release }
    }

    def release: Behavior[Done] = setup { _ =>
      stick2 ! Free
      stick1 ! Free
      acquireFirstStick
    }

    acquireFirstStick
  }

}

class PhilosophersSpec extends AkkaSpec {
  behavior of "philosophers"

  Philosophers() should "always progress" in (
    invariantly(progressIsPossible),
    (root / "Stick1" is Philosophers.stickFree).isInevitable
  )

  Philosophers.deadlock should "deadlock sometimes" in
    potentially(!progressIsPossible)
}
