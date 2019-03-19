name := "scala-modelchecker"

organization := "jentsch.berlin"

homepage := Some(url("https://github.com/Jentsch/modelchecker"))

licenses := Seq("MIT" -> url("https://choosealicense.com/licenses/mit/"))

version := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"

lazy val scalaTestVersion = "3.0.7"

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
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion,
  )

lazy val core = project
  .in(file("core"))
  .settings(
    description := "Internal common functionality shared by the futures and scalaz sub-project, no external API",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
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
      "org.scalatest" %% "scalatest" % scalaTestVersion,
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    examplePackageRef := {
      import scala.meta._
      q"berlin.jentsch.modelchecker.futures"
    }
  )
  .enablePlugins(Example)

lazy val scalaz = project
  .in(file("scalaz"))
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-zio" % "0.16",
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    examplePackageRef := {
      import scala.meta._
      q"berlin.jentsch.modelchecker.scalaz"
    },
    exampleSuperTypes += {
      import scala.meta._
      ctor"_root_.scalaz.zio.DefaultRuntime"
    }
  )
  .enablePlugins(Example)

lazy val akka = project
  .in(file("akka"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.5.21",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
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
