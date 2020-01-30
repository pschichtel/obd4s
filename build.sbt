name := "obd4s"

organization := "tel.schich"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.13.1"

publishMavenStyle := true

resolvers += Resolver.mavenLocal

val blueCoveVersion = "2.1.0"

libraryDependencies ++= Seq(
    "tel.schich" % "javacan" % "3.0.0-SNAPSHOT" changing(),
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2", // nice logging
    "io.suzaku" %% "boopickle" % "1.3.1", // binary serialization
    "com.beachape" %% "enumeratum" % "1.5.15", // nicer enums
    "net.sf.bluecove" % "bluecove" % blueCoveVersion,
    "net.sf.bluecove" % "bluecove-gpl" % blueCoveVersion,
    "org.scream3r" % "jssc" % "2.8.0",
)

scalacOptions ++= Seq("-unchecked", "-deprecation")
