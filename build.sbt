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

val gen = TaskKey[Unit]("gen")

val testGen = project
  .in(file("testGen"))
  .settings(
    description := "Generates tests out of scaladoc code snippets",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "scalameta" % "4.0.0",
      "org.scalameta" %% "contrib" % "4.0.0",
      "com.github.pathikrit" %% "better-files" % "3.0.0"
    ),
    gen := runTask(Compile, "GenerateTests").value
  )

managedSources in Test ++= (target.value / "genTest" ** "*.scala").get

(compile in Test) := (compile in Test).dependsOn(gen in testGen).value

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:Jentsch/modelchecker.git"

enablePlugins(SiteScaladocPlugin)
