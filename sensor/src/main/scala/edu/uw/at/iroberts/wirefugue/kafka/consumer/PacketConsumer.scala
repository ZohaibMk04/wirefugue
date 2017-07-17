package edu.uw.at.iroberts.wirefugue.kafka.consumer

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import edu.uw.at.iroberts.wirefugue.kafka.producer.KafkaKey
import edu.uw.at.iroberts.wirefugue.kafka.serdes.{PacketDeserializer, PacketSerde}
import edu.uw.at.iroberts.wirefugue.pcap.Packet
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.IntegerDeserializer

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/24/17.
  */
object PacketConsumer extends App {
  type PacketRecord = ConsumerRecord[KafkaKey, Array[Byte]]
  val config = ConfigFactory.load("application.conf")

  implicit val system = ActorSystem("stream-consumer-system", config)
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new IntegerDeserializer, new PacketDeserializer)
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  // Separate streams for each partition
  val maxPartitions = 100
  val consumerGroup = Consumer.plainPartitionedSource(consumerSettings, Subscriptions.topics("packets"))

  val done = consumerGroup.map {
    case (topicPartition, source) =>
      val p: Int = topicPartition.partition
      source
        .map { (cr: ConsumerRecord[Integer, Packet]) => cr.value() }
        .filter(_.ip.isDefined)
        .toMat(Sink.foreach(packet => println(s"[$p] $packet")))(Keep.both)
        .run()
  }
    .mapAsyncUnordered(maxPartitions)(_._2)
    .runWith(Sink.ignore)

  Await.result(done, Duration.Inf)

  system.terminate()
}
