name := "scala-modelchecker"

organization := "jentsch.berlin"

homepage := Some(url("https://github.com/Jentsch/modelchecker"))

licenses := Seq("MIT" -> url("https://choosealicense.com/licenses/mit/"))

version := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"

scalacOptions in ThisBuild ++= Seq(
  Opts.compile.unchecked,
  "-feature",
  Opts.compile.deprecation,
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
)

lazy val root = project
  .in(file("."))
  .aggregate(
    core,
    futures,
    scalaz,
    akka
  )
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.6",
  )

lazy val core = project
  .in(file("core"))
  .settings(
    description := "Internal common functionality shared by the futures and scalaz sub-project, no external API",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.6" % Test
    ),
    examplePackageRef := {
      import scala.meta._
      q"berlin.jentsch.modelchecker"
    }
  )
  .enablePlugins(Example)

lazy val futures = project
  .in(file("futures"))
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.6"
    ),
    examplePackageRef := {
      import scala.meta._
      q"berlin.jentsch.modelchecker.futures"
    }
  )
  .enablePlugins(Example)

lazy val scalaz = project
  .in(file("scalaz"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-zio" % "0.10",
      "org.scalatest" %% "scalatest" % "3.0.6" % Test
    )
  )

lazy val akka = project
  .in(file("akka"))
  .settings(
    scalacOptions in Test ++= Seq("-Yrangepos"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalatest" %% "scalatest" % "3.0.6" % Test,
    ),
    examplePackageRef := {
      import scala.meta._
      q"berlin.jentsch.modelchecker.akka"
    }
  )
  .enablePlugins(Example)

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(
    futures,
    akka
  )
  .enablePlugins(JmhPlugin)
  .settings(
    skip in publish := true
  )
