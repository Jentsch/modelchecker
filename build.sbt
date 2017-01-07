name := "modelchecker"

organization := "example"

homepage := Some(url("http://jentsch.berlin/modelchecker/"))

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.1"
scalacOptions ++= Seq(
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1",
  "org.scala-graph" %% "graph-core" % "1.11.4",
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "org.specs2" %% "specs2-core" % "3.8.6" % Test,
  "org.specs2" %% "specs2-html" % "3.8.6" % Test
)

// (testOptions in Test) += Tests.Argument(TestFrameworks.Specs2, "html")

siteSourceDirectory := target.value / "specs2-reports"

makeSite ~= { (f) =>
  val tests = test in Test

  f
}

scalacOptions in Test ++= Seq("-Yrangepos")

ghpages.settings

git.remoteRepo := "git@github.com:Jentsch/modelchecker.git"

enablePlugins(SiteScaladocPlugin)

tutSettings

val tutWebFolder = settingKey[String]("tutWebFolder")

tutWebFolder := "tut"

addMappingsToSiteDir(tut, tutWebFolder)
