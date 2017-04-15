name := "scala-modelchecker"

organization := "jentsch.berlin"

homepage := Some(url("http://jentsch.berlin/modelchecker/"))

licenses := Seq("MIT" -> url("https://choosealicense.com/licenses/mit/"))

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
  "org.scalatest" %% "scalatest" % "3.0.1"
)

scalacOptions in Test ++= Seq("-Yrangepos")

val gen = TaskKey[Unit]("gen")

val testGen = project
  .in(file("testGen"))
  .settings(
    scalaVersion := "2.12.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "scalameta" % "1.7.0",
      "org.scalameta" %% "contrib" % "1.7.0",
      "com.github.pathikrit" %% "better-files" % "3.0.0"
    ),
    gen := runTask(Compile, "GenerateTests").value
  )

sourceDirectory in Test := target.value / "genTest"

(compile in Test) := (compile in Test).dependsOn(gen in testGen).value

ghpages.settings

git.remoteRepo := "git@github.com:Jentsch/modelchecker.git"

enablePlugins(SiteScaladocPlugin)
