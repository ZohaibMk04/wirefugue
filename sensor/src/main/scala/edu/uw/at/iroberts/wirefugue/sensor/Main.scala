package edu.uw.at.iroberts.wirefugue.sensor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import edu.uw.at.iroberts.pcap.PcapFileRaw.LinkType
import edu.uw.at.iroberts.pcap._
import edu.uw.at.iroberts.pcap.overlay.{Ethernet, IPV4Datagram}

/**
  * Created by scala on 5/20/17.
  */
object Main extends App {
  override def main(args: Array[String]) = {
    if (args.length < 1) {
      println("Please specify a filename as the first argument")
      System.exit(1)
    }

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    FileIO.fromPath(Paths.get(args(0)))
      .via(PcapFileParserRaw())
      // .alsoTo(Sink.foreach(println))
      .via(PcapFileParser())
        .collect {
          case p if p.network == LinkType.ETHERNET =>
            Ethernet(p.data)
        }
        .collect {
          case e if e.etherType == EtherType.IPv4.id.toShort =>
            IPV4Datagram(e.payload)
        }
      .alsoTo(Sink.foreach(println))
      .runWith(Sink.onComplete { _ => system.terminate() })
  }
}
