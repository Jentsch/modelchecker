package berlin.jentsch.modelchecker.akka.example

import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.{ActorRef, Behavior}
import berlin.jentsch.modelchecker.akka.AkkaSpec

object VendingMachine {

  val init: Behavior[Nothing] = setup[Nothing] { ctx =>
    val machine = ctx.spawn(vendingMachine, "machine")
    ctx.spawn(customer(machine), "customer")

    empty
  }

  sealed trait VendingMachineMsg
  case object Coin extends VendingMachineMsg
  case object GetBeer extends VendingMachineMsg
  case class GetWater(rec: ActorRef[Water.type]) extends VendingMachineMsg

  sealed trait Drink
  case object Beer extends Drink
  case object Water extends Drink

  lazy val vendingMachine: Behavior[VendingMachineMsg] =
    receiveMessage {
      case Coin =>
        order
    }

  lazy val order: Behavior[VendingMachineMsg] =
    receiveMessage {
      case GetBeer =>
        vendingMachine
      case GetWater(rec) =>
        rec ! Water
        vendingMachine
    }

  def customer(machine: ActorRef[VendingMachineMsg]): Behavior[Drink] =
    setup { ctx =>
      machine ! Coin
      machine ! GetWater(ctx.self)

      receiveMessage { case Water => customer(machine) }
    }

}

class VendingMachineSpec extends AkkaSpec {
  behavior of "a vending machine"

  VendingMachine.init should "eventually accept coins" in
    alwaysGlobally(
      alwaysGlobally(root / "machine" is VendingMachine.vendingMachine)
    )
}
