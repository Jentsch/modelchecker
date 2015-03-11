package actors.sampels.bank

import actors._
import actors.modelchecking.ModelChecking
import org.scalatest.{ PropSpec, Matchers }
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.MatchResult

class ShowAccountSpec
  extends PropSpec
  with Matchers {

  val model = new ShowAccount
  val result = model.check

  import model._
  import result._

  println("States: " + result.graph.nodes.size)

  def accept(states: States) =
    states should contain(initialStates.head)

  def acceptNot(states: States) =
    states should not contain (initialStates.head)

  property("The CTM shouldn't shut down") {
    acceptNot(alwaysGlobally(!(CTM is dead)))
  }

  property("The customer may leaves") {
    accept(existsEventually(Customer is dead))
  }

  property("The customer may see his money") {
    accept(existsEventually(Customer receive "You have some money"))
  }

  property("The customer doesn't see his account until he ask for it") {
    accept(
      !(Customer receive "You have some money") existsUntil (CTM receive "Account"))
  }

  property("The customer could see the main screen after seeing his account") {
    accept(
      alwaysGlobally((Customer receive "You have some money") ->
        existsEventually(Customer receive "Welcome")))
  }

  property("After returning the card the CTM shouldn't show the account") {
    accept(alwaysGlobally(
      (Customer receive "card") ->
        alwaysGlobally(!(Customer receive "You have some money"))))
  }

  property("The customer could see the account infinitly many times") {
    accept(alwaysGlobally(
      (Customer is Customer.init) -> existsEventually(Customer is Customer.waiting) &
        (Customer is Customer.waiting) -> existsEventually(Customer is Customer.init)))
  }
}

/**
 * Following scenario:
 * * A customer insert his card into a CTM
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
        CTM ! choose("Exit", "Account")
        become(waiting)
      case "card" =>
        become(dead)
    }

    val waiting: Behaviour = {
      case "You have some money" =>
        CTM ! "Ok"
        become(init)
      case "card" =>
        become(dead)
    }
  }

}
