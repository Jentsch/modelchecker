name := "scala-modelchecker"

organization := "jentsch.berlin"

homepage := Some(url("https://github.com/Jentsch/modelchecker"))

licenses := Seq("MIT" -> url("https://choosealicense.com/licenses/mit/"))

version := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.7"

scalacOptions in ThisBuild ++= Seq(
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

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:Jentsch/modelchecker.git"

enablePlugins(SiteScaladocPlugin)

lazy val root = project
  .in(file("."))
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5"
  )
  .aggregate(
    futures,
    scalaz,
    akka
  )

lazy val futures = project
  .in(file("futures"))
  .settings(
    scalacOptions in Test ++= Seq("-Yrangepos"),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5",
    examplePackageRef := {
      import scala.meta._
      q"ecspec"
    }
  )
  .enablePlugins(Example)

lazy val scalaz = project
  .in(file("scalaz"))

lazy val akka = project
  .in(file("akka"))

lazy val benchmarks = project
  .in(file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .settings(
    skip in publish := true
  )
  .aggregate(
    akka
  )
