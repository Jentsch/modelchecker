package berlin.jentsch.modelchecker.akka.example

import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.{ActorRef, Behavior}
import berlin.jentsch.modelchecker.akka.example.Ferryman._
import berlin.jentsch.modelchecker.akka.{AkkaSpec, NonDeterministic, Property}

import scala.collection.immutable.SortedSet

object Ferryman {

  object Item extends Enumeration {
    val wolf, goat, cabbage = Value
  }
  type Item = Item.Value
  val (wolf, goat, cabbage) = (Item.wolf, Item.goat, Item.cabbage)

  def init: Behavior[Unit] = setup[Unit] { ctx =>
    import ctx._

    val allItems = SortedSet(Item.values.toSeq: _*)
    val start = spawn(watchedBank(allItems), "start")
    val target = spawn(unwatchedBank(SortedSet.empty), "target")

    val man = spawn(ferryman(start, target), "ferryman")

    man ! Options(allItems)

    empty
  }

  sealed trait Bank
  case class Drop(man: ActorRef[Options], item: Option[Item]) extends Bank
  case class Take(item: Option[Item]) extends Bank

  def unwatchedBank(items: SortedSet[Item]): Behavior[Bank] =
    receiveMessage {
      case Drop(man, newItem) =>
        val newItems = items ++ newItem
        man ! Options(newItems)
        watchedBank(newItems)
    }

  def watchedBank(items: SortedSet[Item]): Behavior[Bank] =
    receiveMessage {
      case Take(item) =>
        unwatchedBank(items -- item)
    }

  case class Options(options: SortedSet[Item])

  def ferryman(
      bank1: ActorRef[Bank],
      bank2: ActorRef[Bank]
  ): Behavior[Options] =
    receive {
      case (ctx, Options(options)) =>
        val nd = ctx.system.extension(NonDeterministic)

        val chosen = nd.oneOf(None :: options.toList.map(Some(_)))

        bank1 ! Take(chosen)

        bank2 ! Drop(ctx.self, chosen)

        ferryman(bank2, bank1)
    }

}

class FerrymanSpec extends AkkaSpec {
  behavior of "ferryman"

  val badStates: Property =
    ((root / "target") is unwatchedBank(SortedSet(wolf, goat))) &
      ((root / "target") is unwatchedBank(SortedSet())) &
      ((root / "start") is unwatchedBank(SortedSet(wolf, goat))) &
      ((root / "start") is unwatchedBank(SortedSet(goat, cabbage)))

  Ferryman.init should "always progress" in (
    existsUntil(
      !badStates,
      (root / "target") is watchedBank(SortedSet(wolf, goat, cabbage))
    ),
    invariantly(
      potentially(
        root / "start" is watchedBank(SortedSet(wolf, goat, cabbage))
      )
    )
  )
}
