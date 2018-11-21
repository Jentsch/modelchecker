name := "scala-modelchecker"

organization := "jentsch.berlin"

homepage := Some(url("http://jentsch.berlin/modelchecker/"))

licenses := Seq("MIT" -> url("https://choosealicense.com/licenses/mit/"))

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.7"

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
  "org.scalatest" %% "scalatest" % "3.0.5"
)

scalacOptions in Test ++= Seq("-Yrangepos")

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:Jentsch/modelchecker.git"

enablePlugins(SiteScaladocPlugin)

enablePlugins(Example)

examplePackageRef := {
  import scala.meta._
  q"ecspec"
}
