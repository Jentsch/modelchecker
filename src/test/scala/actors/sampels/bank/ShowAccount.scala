package actors.sampels.bank

import actors._
import org.scalatest.{PropSpec, Matchers}

class ShowAccountSpec
  extends PropSpec
  with Matchers {

  val model = new ShowAccount
  val result = model.check

  import model._
  import result._

  result should be(1)
}

/**
 * Following scenario:
 * * A customer insert his card into CTM
 * * Sees a welcome screen and want to see his account
 * * He dies happy if he have some money
 *
 * He could always miss a screen but never his rejected card.
 */
class ShowAccount extends Bank with ModelChecking {

  val Customer = new Actor {
    override def creation() = {
      CTM ! "card"
    }

    val init: Behaviour = {
      case "Welcome" =>
        CTM ! "Account"
        become(waiting)
      case "card" =>
        become(dead)
    }

    val waiting: Behaviour = {
      case "You have some money" =>
        CTM ! "Ok"
        become(leaving)
      case "card" =>
        become(dead)
    }

    val leaving: Behaviour = {
      case "Welcome" =>
        CTM ! "Exit"
      case "card" =>
        become(dead)
    }
  }

}
