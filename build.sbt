name := "obd4s"

organization := "tel.schich"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.6"

publishMavenStyle := true

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
    "tel.schich" % "javacan" % "1.3-SNAPSHOT",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0", // nice logging
    "io.suzaku" %% "boopickle" % "1.3.0", // binary serialization
    "com.beachape" %% "enumeratum" % "1.5.13", // nicer enums
)
