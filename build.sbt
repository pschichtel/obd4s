import ReleaseTransformations.*

name := "obd4s"
organization := "tel.schich"

homepage := Some(url("https://github.com/pschichtel/obd4s"))
scmInfo := Some(ScmInfo(url("https://github.com/pschichtel/obd4s"), "git@github.com:pschichtel/obd4s.git"))
developers := List(Developer("pschichtel", "Phillip Schichtel", "phillip@schich.tel", url("https://schich.tel")))
licenses += ("MIT", url("https://raw.githubusercontent.com/pschichtel/obd4s/master/LICENSE"))

versionScheme := Some("semver-spec")
resolvers += Resolver.mavenLocal
publishMavenStyle := true
publishTo := Some(
    if (isSnapshot.value)
        Opts.resolver.sonatypeOssSnapshots.head
    else
        Opts.resolver.sonatypeStaging
)

scalaVersion := "3.6.2"

val blueCoveVersion = "2.1.0"
val javacanVersion = "3.5.0"

libraryDependencies ++= Seq(
    "tel.schich" % "javacan-core" % javacanVersion,
    "tel.schich" % "javacan-epoll" % javacanVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5", // nice logging
    "net.sf.bluecove" % "bluecove" % blueCoveVersion,
    "net.sf.bluecove" % "bluecove-gpl" % blueCoveVersion,
    "io.github.java-native" % "jssc" % "2.9.6",
    "org.scalacheck" %% "scalacheck" % "1.18.1" % "test"
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
