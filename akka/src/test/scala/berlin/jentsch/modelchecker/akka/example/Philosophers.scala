package berlin.jentsch.modelchecker.akka.example

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors._

import scala.concurrent.Await
import scala.concurrent.duration._

object Philosophers {
  def apply(): Behavior[Unit] = setup { ctx =>
    val stick1 = ctx.spawn(stick, "Stick1")
    val stick2 = ctx.spawn(stick, "Stick2")

    ctx.spawn(philosophers(stick1, stick2), "P1")
    ctx.spawn(philosophers(stick1, stick2), "P2")

    empty
  }

  sealed trait StickMessages
  case class Req(sender: ActorRef[StickAck]) extends StickMessages
  case class Free(sender: ActorRef[StickAck]) extends StickMessages

  object StickAck
  type StickAck = StickAck.type

  def stick: Behavior[StickMessages] = stickFree

  def stickFree: Behavior[StickMessages] = receiveMessage {
    case Req(sender) =>
      sender ! StickAck
      stickInUse
  }

  def stickInUse: Behavior[StickMessages] = receiveMessage {
    case Free(sender) =>
      sender ! StickAck
      stickFree
    case Req(sender) =>
      stickRequested(sender)
  }

  def stickRequested(pendingRequest: ActorRef[StickAck]): Behavior[StickMessages] =
    receiveMessage {
      case Free(sender) =>
        sender ! StickAck
        pendingRequest ! StickAck
        stickInUse
    }

  def philosophers(stick1: ActorRef[StickMessages],
                   stick2: ActorRef[StickMessages]): Behavior[StickAck] =
    setup { ctx =>
      stick1 ! Req(ctx.self)
      ctx.log.info("Request 1 to " + stick1.path)
      receive {
        case (ctx, StickAck) =>
          stick2 ! Req(ctx.self)
          ctx.log.info("Request 2 to " + stick2.path)
          receive {
            case (ctx, StickAck) =>
              ctx.log.info("Release sticks")
              release(stick1, stick2)
          }
      }
    }

  def release(stick1: ActorRef[StickMessages],
              stick2: ActorRef[StickMessages]): Behavior[StickAck] =
    setup { ctx =>
      stick2 ! Free(ctx.self)
      stick1 ! Free(ctx.self)
      philosophers(stick1, stick2)
    }

}

object PhilosophersSpec {
  def main(args: Array[String]): Unit = {
    println("Start")
    val sys = ActorSystem(Philosophers(), "TestSys")
    Thread.sleep(50)
    Await.ready(sys.terminate(), 100.milliseconds)
  }
}