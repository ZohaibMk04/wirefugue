package edu.uw.at.iroberts.wirefugue.sensor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import com.typesafe.config.ConfigFactory
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw.LinkType
import edu.uw.at.iroberts.wirefugue.pcap._
import edu.uw.at.iroberts.wirefugue.kafka.producer.{KafkaKey, PacketProducer}
import edu.uw.at.iroberts.wirefugue.kafka.serdes.PacketSerializer
import edu.uw.at.iroberts.wirefugue.protocol.overlay.{Ethernet, IPV4Datagram}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, IntegerSerializer}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/20/17.
  */
object Main {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Please specify a filename as the first argument")
      System.exit(1)
    }

    val config = ConfigFactory.load("application.conf")

    implicit val system = ActorSystem("stream-producer-system", config)
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings[Integer, Packet](system, None, None)

    val doneF = PcapSource(Paths.get(args(0)).toUri)
      .filter( p => p.network == LinkType.ETHERNET && p.ip.isDefined )
      .map( packet => new ProducerRecord[Integer, Packet]("packets", packet.key.##, packet))
      .map( pr => new ProducerMessage.Message[Integer, Packet, Unit](pr, ()))
      .via(Producer.flow(producerSettings))
      .runWith(Sink.foreach(println))

    try {
      Await.ready(doneF, 10 seconds)
    }
    finally {
      system.terminate()
    }
  }
}
