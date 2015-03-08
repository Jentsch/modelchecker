package actors.sampels.bank

import actors._

/** Basic model of CTM */
trait Bank extends ActorSystem {
  def Customer: Actor

  val CTM = new Actor {
    val init: Behaviour = {
      case "card" =>
        Customer ! "Welcome"
        self ! "timeout"
        become(mainScreen)
      case _ =>
        ()
    }

    val mainScreen: Behaviour = {
      case "Account" =>
        Customer ! "You have some money"
        become(waiting)
      case "Exit" | "timeout" =>
        Customer ! "card"
        become(init)
      case _ =>
        ()
    }
    
    val waiting: Behaviour = {
      case "Ok" =>
        become(mainScreen)
      case "timeout" =>
        Customer ! "card"
        become(mainScreen)
      case _ =>
        ()
    }
  }

}
