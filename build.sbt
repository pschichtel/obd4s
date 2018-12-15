name := "obd4s"

organization := "tel.schich"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.8"

publishMavenStyle := true

resolvers += Resolver.mavenLocal

val blueCoveVersion = "2.1.0"

libraryDependencies ++= Seq(
    "tel.schich" % "javacan" % "1.3-SNAPSHOT",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0", // nice logging
    "io.suzaku" %% "boopickle" % "1.3.0", // binary serialization
    "com.beachape" %% "enumeratum" % "1.5.13", // nicer enums
    "net.sf.bluecove" % "bluecove" % blueCoveVersion,
    "net.sf.bluecove" % "bluecove-gpl" % blueCoveVersion,
    "org.scream3r" % "jssc" % "2.8.0",
)
