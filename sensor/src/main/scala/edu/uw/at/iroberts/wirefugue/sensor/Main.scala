package edu.uw.at.iroberts.wirefugue.sensor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import com.typesafe.config.ConfigFactory
import edu.uw.at.iroberts.pcap.PcapFileRaw.LinkType
import edu.uw.at.iroberts.pcap._
import edu.uw.at.iroberts.pcap.kafkaproducer.KafkaKey
import edu.uw.at.iroberts.pcap.overlay.{Ethernet, IPV4Datagram}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

/**
  * Created by scala on 5/20/17.
  */
object Main extends App {
  override def main(args: Array[String]) = {
    if (args.length < 1) {
      println("Please specify a filename as the first argument")
      System.exit(1)
    }

    val config = ConfigFactory.load("application.conf")

    implicit val system = ActorSystem("stream-producer-system", config)
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings(system, KafkaKey.serializer, new ByteArraySerializer)
    //val producerSettings = ProducerSettings(system, KafkaKey.serializer, new StringSerializer)
      .withBootstrapServers("localhost:9092,localhost:9093,localhost:9094")

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
      .map( dg => new ProducerRecord[KafkaKey, Array[Byte]]("packets", KafkaKey.fromIPV4Datagram(dg), dg.bytes.toArray))
      //.map( dg => new ProducerRecord[KafkaKey, String]("packets", KafkaKey.fromIPV4Datagram(dg), dg.toString))
      .alsoTo(Producer.plainSink(producerSettings))
      .runWith(Sink.onComplete { _ => system.terminate() })
  }
}
