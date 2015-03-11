# Modelchecker (Alles noch in Arbeit)

Prototyp eines Modelcheckers

## Ziele

Der Modelchecker soll echte™ verteilte Anwendungen überprüfen können. Echt™ wird hier zur Abgrenzung von Modellen verwendet.

Echte™ Software zeichnet unter anderem folgendes aus:

1. Sie ist lauffähig
2. kann vorhandene Programmbibliotheken einbinden
3. verwaltet komplexe Datenstrukturen zusammen gesetzten aus: Integer-Derivaten (long, char, bool ...), Algebraischen Datentypen
4. es gibt mindestens eine etablierte Lösungen für asynchrone Datenverarbeitung, von der ausgegangen werden kann, dass sie sich spezifikationsgemäß verhält. Beispiele wäre unter anderen: Transaktionen, Pipes, Aktoren, Monitore, Futures
5. Sie hat eine modularen Aufbau.
6. Es kann eine Problemdomäne geben, die außerhalb der Informatik liegt.

## Beispiel

Ein Anwender will ein Zusatzfeature für seine Anwendung buchen. Um die Bezahlung zu organisieren, wird ein bereits exsistierender Bezahldienst verwendet.

Da all Instanzen (Anwender, Anwendung, Bezahldienst) nur per Netzwerk verbunden sind, muss davon ausgegangen werden, dass einzelne Anfragen verloren gehen.

Nun könnten folgende (nicht testbaren) Anforderungen könnten von Interesse sein:
* Wenn keine Anfragen verloren gehen (d.h. auch, der Bezahldienst ist nicht offline), dann gibt es für den Kunden keine Möglichkeit die Zusatzfeatures zu verwenden ohne zuvor bezahlt zu haben.
* Selbst wenn Bezahldienst zu einen beliebigen Zeitpunkt nicht mehr erreichbar ist, kann es nicht dazu kommen, dass der Anwender Geld verloren hat ohne das Zusatzfeature bekommen zu haben. Das Gegenteil ist aber erlaubt. (Er hat das Feature bekommen, musste aber dafür nicht bezahlen)
* Zwei Benutzer können sich nicht gegenseitig beeinflussen.

## Umsetzung

In diesem Projekt wird versucht die Anforderung mittels eines Aktorenmodells zu erfüllen. Die gleiche Idee wird auch von Rebecca und McErlang umgesetzt. Auf der JVM wurde vergleichbares bisher nicht untersucht.

## Einführung

Es gibt zwei verschieden Actor-Systeme: Integrated and Model Checking. Sie teilen sich beide die Grundfunktionalitäten von Aktoren.
  
### Integrated System

Dies sind lauffähige Anwendungen, die Zugriff auf das Netzwerk und die Festplatte haben. Dieser Zugriff wird mittels IO-Aktoren geregelt.

Die Implementierung hierfür ist noch nicht laufähig.

### Model Checking

Zusätzlich zu den Basis Funktionen stehen den Aktoren eine ```choose(...)``` für nicht deterministische Entscheidungen zur Verfügung.

Das System selbst stellt eine ```check``` Funktion bereit, die dass Ergebnis zurückgibt. Das Ergebniss-Objekt stellt wiederum weitere Methoden zur Überprüfung von CTL-Eigenschaften bereit. Als atomare Aussagen stehen die Zustände/ das Verhalten der Aktoren zur Verfügung als auch dem Empfang von Nachrichten. Die Datei ```ShowAccount.scala``` zeigt einige Beispiele.

### Systemkomposition

Es werden Traits verwendet, um die Verschiedenen Systemmodelle zu verwalten. Es sollte eine gemeinse Basis geben, die direkt von ```ActorSystem``` erbt. In dieser werden all Systemkomponenten und Abhänigkeiten notiert.

Beispiel:
```scala
trait AbstractSystem extends ActorSystem {
  def configuration: Actor
  def database: Actor
  def mainService: Actor
  def helper: Actor
  def userInterface: Actor
}

trait SystemImplementation extends AbstractSystem {
  lazy val mainService = ???
  lazy val help = ???
}

object RunningSystem extends SystemImplementation with IntegratedSystem {
  lazy val configuration = LocalJSON("/opt/system/config.json")
  lazy val database = MySQL("mysql.company.com", "user", "password")
  lazy val userInterface = HTTP_Server(Templates)
}

object CheckSystem extends SystemImplementation with ModelChecking {
  lazy val configuration = ???
  lazy val database = new Actor {
    def init: Behaviour = {
      case "SELECT * FROM users" =>
        mainService ! choose(List.empty, List("John"), List("John", "Sven"))
    }
  override lazy val helper = DeadActor
}
```
Mit ```CheckSystem``` kann anschließend die Anwendung gemodelcheckt werden.