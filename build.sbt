name := """h2"""

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.10.4", "2.11.2")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.3" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
  "com.assembla.scala-incubator" %% "graph-core" % "1.9.1"
)

initialCommands := "import example._"
