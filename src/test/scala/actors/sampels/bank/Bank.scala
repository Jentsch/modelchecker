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
    }

    val mainScreen: Behaviour = {
      case "Account" =>
        Customer ! "card"
      case "timeout" =>
        Customer ! "card"
    }
  }

}
