package edu.uw.at.iroberts.wirefugue.pcap

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

/** Prints out the packets in http.cap
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  */
object PcapFileDemo extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  FileIO.fromPath(Paths.get("src/main/resources/http.cap"))
    .via(PcapFileParserRaw())
    // .alsoTo(Sink.foreach(println))
    .via(PcapFileParser())
    .via(PacketLineFormatter())
    .alsoTo(Sink.foreach(println))
    .runWith(Sink.onComplete { _ => system.terminate() })

}
