name := "obd4s"

organization := "tel.schich"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.13.3"

publishMavenStyle := true

resolvers += Resolver.mavenLocal

val blueCoveVersion = "2.1.0"

libraryDependencies ++= Seq(
    "tel.schich" % "javacan" % "2.1.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2", // nice logging
    "io.suzaku" %% "boopickle" % "1.3.3", // binary serialization
    "com.beachape" %% "enumeratum" % "1.6.1", // nicer enums
    "net.sf.bluecove" % "bluecove" % blueCoveVersion,
    "net.sf.bluecove" % "bluecove-gpl" % blueCoveVersion,
    "io.github.java-native" % "jssc" % "2.9.2",
)

scalacOptions ++= Seq("-unchecked", "-deprecation")
