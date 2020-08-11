import ReleaseTransformations._

name := "obd4s"
organization := "tel.schich"

homepage := Some(url("https://github.com/pschichtel/obd4s"))
scmInfo := Some(ScmInfo(url("https://github.com/pschichtel/obd4s"), "git@github.com:pschichtel/obd4s.git"))
developers := List(Developer("pschichtel", "Phillip Schichtel", "phillip@schich.tel", url("https://schich.tel")))
licenses += ("MIT", url("https://raw.githubusercontent.com/pschichtel/obd4s/master/LICENSE"))

resolvers += Resolver.mavenLocal
publishMavenStyle := true
publishTo := Some(
    if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
    else
        Opts.resolver.sonatypeStaging
)

scalaVersion := "2.13.3"

val blueCoveVersion = "2.1.0"

libraryDependencies ++= Seq(
    "tel.schich" % "javacan" % "2.1.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2", // nice logging
    "io.suzaku" %% "boopickle" % "1.3.3", // binary serialization
    "com.beachape" %% "enumeratum" % "1.6.1", // nicer enums
    "net.sf.bluecove" % "bluecove" % blueCoveVersion,
    "net.sf.bluecove" % "bluecove-gpl" % blueCoveVersion,
    "io.github.java-native" % "jssc" % "2.9.2",
    "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeRelease"),
    pushChanges
)
