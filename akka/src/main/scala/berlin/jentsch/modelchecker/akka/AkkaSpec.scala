package berlin.jentsch.modelchecker.akka

import akka.actor.typed.Behavior
import org.scalatest.FlatSpec

trait AkkaSpec extends FlatSpec with PropertySyntax {

  private def test(behavior: Behavior[_], properties: Property): Unit = ()

  implicit class BehaviorShould(behavior: Behavior[_]) {
    def should(description: String): InWord = new InWord(behavior, description)
  }

  class InWord(behavior: Behavior[_], description: String) {
    def in(property: Property): Unit =
      it should description in {
        test(behavior, property)
      }
  }

}
