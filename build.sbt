name := """modelchecker"""

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0",
  "com.assembla.scala-incubator" %% "graph-core" % "1.11.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.10"
)
