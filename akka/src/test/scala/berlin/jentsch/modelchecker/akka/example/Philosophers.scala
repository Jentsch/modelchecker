package berlin.jentsch.modelchecker.akka.example

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors._
import berlin.jentsch.modelchecker.akka._

object Philosophers {
  def apply(): Behavior[Unit] = setup { ctx =>
    val stick1 = ctx.spawn(stick, "Stick1")
    val stick2 = ctx.spawn(stick, "Stick2")

    ctx.spawn(philosophers(stick1, stick2), "Philosopher1")
    ctx.spawn(philosophers(stick1, stick2), "Philosopher2")

    empty
  }

  sealed trait StickMessages
  case class Req(sender: ActorRef[StickAck]) extends StickMessages
  case class Free(sender: ActorRef[StickAck]) extends StickMessages

  case object StickAck
  type StickAck = StickAck.type

  def stick: Behavior[StickMessages] = stickFree

  lazy val stickFree: Behavior[StickMessages] = receiveMessage {
    case Req(sender) =>
      sender ! StickAck
      stickInUse
  }

  lazy val stickInUse: Behavior[StickMessages] = receiveMessage {
    case Free(sender) =>
      sender ! StickAck
      stickFree
    case Req(sender) =>
      stickRequested(sender)
  }

  def stickRequested(
      pendingRequest: ActorRef[StickAck]
  ): Behavior[StickMessages] =
    receiveMessage {
      case Free(sender) =>
        sender ! StickAck
        pendingRequest ! StickAck
        stickInUse
    }

  def philosophers(
      stick1: ActorRef[StickMessages],
      stick2: ActorRef[StickMessages]
  ): Behavior[StickAck] = {

    def acquireFirstStick: Behavior[StickAck] = setup { ctx =>
      stick1 ! Req(ctx.self)
      receive { case (ctx, StickAck) => acquireSecondStick }
    }

    def acquireSecondStick: Behavior[StickAck] = setup { ctx =>
      stick2 ! Req(ctx.self)
      receive { case (ctx, StickAck) => release }
    }

    def release: Receive[StickAck] = receive {
      case (ctx, StickAck) =>
        stick2 ! Free(ctx.self)
        stick1 ! Free(ctx.self)
        acquireFirstStick
    }

    acquireFirstStick
  }

}

class PhilosophersSpec extends AkkaSpec {
  behavior of "philosophers"

  Philosophers() should "always progress" in (
    root is Philosophers(),
    alwaysNext(root is empty)
  )

  Philosophers() should "initially have no sticks" in (
    !(root / "Stick1" is Philosophers.stick),
    root / "Stick1" is stopped
  )
}
