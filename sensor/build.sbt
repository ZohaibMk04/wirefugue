name := """wirefugue"""

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {

  val akkaVersion = "2.4.14"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

    "org.apache.kafka" % "kafka-clients" % "0.10.2.0",

    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )

}

fork in run := true
