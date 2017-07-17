package edu.uw.at.iroberts.wirefugue.pcap

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

/** Prints out the packets in http.cap
  *
  * Compare with the output of:
  * $ TZ=UTC tcpdump -tttt -S -nn -r http.cap
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  */
object PcapFileDemo extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val uri = getClass.getResource("/http.cap").toURI
  PcapSource(uri)
    .alsoTo(Sink.foreach(println))
    .runWith(Sink.onComplete { _ => system.terminate() })

}
