name := """modelchecker"""

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "com.assembla.scala-incubator" %% "graph-core" % "1.9.1"
)

