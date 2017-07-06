package edu.uw.at.iroberts.wirefugue.kafka.producer

import akka.NotUsed
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Sink}
import edu.uw.at.iroberts.wirefugue.kafka.serdes.PacketSerde
import edu.uw.at.iroberts.wirefugue.pcap.Packet
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/1/17.
  */
object PacketProducer {
  def plainSink(topic: String, producerSettings: ProducerSettings[AnyRef, Packet]): Sink[Packet, NotUsed] =
  Flow[Packet].map( p => new ProducerRecord[AnyRef, Packet](topic, null, p))
    .to(Producer.plainSink(producerSettings))
}

object PacketProducerExample extends App {
  import com.typesafe.config._
  import akka.actor._
  import akka.stream._
  import akka.stream.scaladsl._
  import java.nio.file._

  import edu.uw.at.iroberts.wirefugue.pcap._

  override def main(args: Array[String]) = {
    if (args.length < 1) {
      println("Please specify a filename as the first argument")
      System.exit(1)
    }

    val config = ConfigFactory.load("application.conf")

    implicit val system = ActorSystem("stream-producer-system", config)
    implicit val materializer = ActorMaterializer()

    val producerSettings: ProducerSettings[AnyRef, Packet] =
      ProducerSettings(system, None, Some(new PacketSerde().serializer()))
      .withBootstrapServers("localhost:9092,localhost:9093,localhost:9094")

    val uri = Paths.get(args(0)).toUri
    PcapSource(uri)
      .alsoTo(PacketProducer.plainSink("packets", producerSettings))
      .runWith(Sink.onComplete { _ => system.terminate() })
  }

}
