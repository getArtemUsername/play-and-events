name := """play-event-sourcing-starter"""
val commonSettings = Seq(
  organization := "com.appliedscala.streaming",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.8"
)
lazy val events = (project in file("events")).settings(commonSettings)
  .settings(
    Seq(libraryDependencies := Seq(
      "joda-time" % "joda-time" % "2.9.4",
      "com.typesafe.play" %% "play-json" % "2.5.9"
    ))
  )

lazy val root = (project in file(".")).settings(commonSettings)
  .enablePlugins(PlayScala)
  .aggregate(events)
  .dependsOn(events)

pipelineStages := Seq(digest)
routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  jdbc,
  evolutions,
  "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided",
  "org.postgresql" % "postgresql" % "9.4.1207.jre7",
  "org.scalikejdbc" %% "scalikejdbc" % "2.4.2",
  "org.scalikejdbc" %% "scalikejdbc-config" % "2.4.2",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "de.svenkubiak" % "jBCrypt" % "0.4.1"
)
