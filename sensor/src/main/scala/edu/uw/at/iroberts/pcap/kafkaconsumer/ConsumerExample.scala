package edu.uw.at.iroberts.pcap.kafkaconsumer

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import edu.uw.at.iroberts.pcap.kafkaproducer.KafkaKey
import edu.uw.at.iroberts.pcap.overlay.IPV4Datagram
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/24/17.
  */
object ConsumerExample extends App {
  type PacketRecord = ConsumerRecord[KafkaKey, Array[Byte]]
  val config = ConfigFactory.load("application.conf")

  implicit val system = ActorSystem("stream-producer-system", config)
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, KafkaKey.deserializer, new ByteArrayDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val partition = 1
  val offset: Long = 0

  val subscription = Subscriptions.assignmentWithOffset(
    new TopicPartition("packets", partition) -> offset
  )
  val done =
    Consumer.plainSource(consumerSettings, subscription)
      .map(rec => IPV4Datagram(rec.value()))
      .runWith(Sink.foreach(println))

  Await.result(done, Duration.Inf)

  system.terminate()
}
