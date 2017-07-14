name := """wirefugue-sensor"""

organization := "edu.uw.at.iroberts"

version := "0.0.1"

scalaVersion := "2.12.2"

libraryDependencies ++= {

  val akkaVersion = "2.4.18"

  val protobufVersion = "3.3.1"

  Seq(
    "org.slf4j" % "slf4j-simple" % "1.7.25",

    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

    "org.apache.kafka" % "kafka-clients" % "0.10.2.1",
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",

    "com.google.protobuf" % "protobuf-java" % protobufVersion,

    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  )

}

fork in run := true

// The following was copied from https://scalapb.github.io
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// (optional) If you need scalapb/scalapb.proto or anything from
// google/protobuf/*.proto
libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
// The preceding was copied from https://scalapb.github.io

