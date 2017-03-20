# Model checker (Work in progress)

[Website](http://jentsch.berlin/modelchecker/)

Prototype of a explicit state model checker based upon the Scala ExecutionContext.

## Development

This is a plain sbt project. Use `sbt test` to execute all tests.

## Documentation

The website is mostly generated by the test files in /src/test/scala/. index.scala generates initial landing page. Use `sbt previewSite` to see a assembled preview. With `ghpagesPushSite` the result can be uploaded.


## Ziele

Der Modelchecker soll echte™ verteilte Anwendungen überprüfen können. Echt™ wird hier zur Abgrenzung von Modellen verwendet.

Echte™ Software zeichnet unter anderem folgendes aus:

1. Sie ist lauffähig
2. kann vorhandene Programmbibliotheken einbinden
3. verwaltet komplexe Datenstrukturen
4. es gibt mindestens eine etablierte Lösungen für asynchrone Datenverarbeitung, von der ausgegangen werden kann, dass sie sich spezifikationsgemäß verhält. Beispiele wäre unter anderen: Transaktionen, Pipes, Aktoren, Monitore, Futures
5. Sie hat eine modularen Aufbau.
6. Es kann eine Problemdomäne geben, die außerhalb der Informatik liegt.
