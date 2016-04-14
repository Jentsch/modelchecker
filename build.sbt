name := """modelchecker"""

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.assembla.scala-incubator" %% "graph-core" % "1.9.1"
)

