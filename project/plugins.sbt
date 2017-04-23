resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.0")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.6.8")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.4.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
