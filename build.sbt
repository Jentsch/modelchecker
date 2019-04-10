name := "scala-modelchecker"

organization := "jentsch.berlin"

homepage := Some(url("https://github.com/Jentsch/modelchecker"))

licenses := Seq("MIT" -> url("https://choosealicense.com/licenses/mit/"))

version := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"

lazy val scalaTestVersion = "3.0.7"

scalacOptions ++= Seq(
  Opts.compile.unchecked,
  "-feature",
  Opts.compile.deprecation,
  "-Xfuture",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
)

scalacOptions ++= {
  if (scalaVersion.value startsWith "2.12")
    Seq("-Yno-adapted-args", "-Ywarn-unused-import", "-Ywarn-unused")
  else
    Seq()
}

lazy val root = project
  .in(file("."))
  .aggregate(
    core,
    futures,
    scalaz,
    akka,
    benchmarks,
    jpf,
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
    },
    crossScalaVersions += "2.10.7"
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
      "org.scalaz" %% "scalaz-zio" % "0.19",
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
      "com.typesafe.akka" %% "akka-actor-typed" % "2.5.22",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalatest" %% "scalatest" % scalaTestVersion,
      "org.scala-graph" %% "graph-core" % "1.12.5",
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

lazy val jpf = project
  .in(file("jpf"))
  .dependsOn(core)
  .settings(
    description := "Generates that can be used by the JavaPathfinder",
    // JavaPathfinder can't parse newer Byte-Code
    crossScalaVersions += "2.10.7",
    skip in publish := true,
  )
