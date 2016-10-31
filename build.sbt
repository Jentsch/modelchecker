lazy val root = (project in file(".")).configs(Examples)

name := """modelchecker"""

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0",
  "com.assembla.scala-incubator" %% "graph-core" % "1.11.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.10",
  "org.scalaz" %% "scalaz-core" % "7.2.6",
  "org.specs2" %% "specs2-core" % "3.8.5" % "test",
  "org.specs2" %% "specs2-html" % "3.8.5" % "test"
)

// (testOptions in Test) += Tests.Argument(TestFrameworks.Specs2, "html")

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val Examples = config("examples").extend(Runtime)
