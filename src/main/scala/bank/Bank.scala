package bank

import actors._

trait Bank extends ActorSystem {
  def Customer: Actor

  val CTM = new Actor {
    val init: Behaviour = {
      case "card" =>
        Customer ! "Welcome"
        self ! "timeout"
        become(mainScreen)
    }

    val mainScreen: Behaviour = {
      case "Account" =>
        Customer ! "card"
      case "timeout" =>
        Customer ! "card"
    }
  }

}

class ShowAccount extends Bank with ModelChecking {

  val Customer = new Actor {
    override def creation = {
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


