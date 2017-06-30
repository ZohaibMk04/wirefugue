name := """wirefugue"""

organization := "edu.uw.at.iroberts"

version := "0.0.1"

scalaVersion := "2.12.2"

lazy val commonSettings = Seq(
  version := "0.0.1",
  organization := "edu.uw.at.iroberts",
  scalaVersion := "2.12.2",
  test in assembly := {},
  mainClass in assembly := Some("edu.uw.at.iroberts.wirefugue.sensor.Main")
)

lazy val sensor = (project in file("sensor")).
  settings(commonSettings: _*).
  settings(
    mainClass in assembly := Some("edu.uw.at.iroberts.wirefugue.sensor.Main")
  )

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(sensor)
  .dependsOn(sensor)
