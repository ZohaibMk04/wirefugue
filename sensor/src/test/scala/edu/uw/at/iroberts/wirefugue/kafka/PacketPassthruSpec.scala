package edu.uw.at.iroberts.wirefugue.kafka

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import edu.uw.at.iroberts.wirefugue.kafka.producer.PacketProducer
import edu.uw.at.iroberts.wirefugue.kafka.serdes.{PacketDeserializer, PacketSerializer}
import edu.uw.at.iroberts.wirefugue.pcap.{Packet, PcapSource}
import edu.uw.at.iroberts.wirefugue.protobufs.packet.{Packet => ProtobufPacket}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.Cluster
import org.apache.kafka.common.serialization.{IntegerDeserializer, IntegerSerializer}
import org.apache.kafka.common.utils.Utils
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * An "end-to-end" test that demonstrates reading from a capture file,
  * streaming to a Kafka producer, Protocol Buffer serialization and
  * deserialization, reading a stream-of-sources from a consumer group,
  * and proper partitioning.
  *
  * It uses scalatest-embedded-kafka
  * (https://github.com/manub/scalatest-embedded-kafka) to launch a real
  * Zookeeper node and one broker.
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 7/11/17.
  */
class PacketPassthruSpec extends FlatSpec with Matchers with EmbeddedKafka {
  /**
    * Display a map from partition numbers to a sequence of packets. The packets
    * are displayed as hashes for easy comparison. Elements with empty sequences
    * are not displayed.
    *
    * @param m the Map to display
    * @return a multiline string
    */
  private def formatPacketMap(m: Map[Int, Seq[Packet]]): String = {
    val lines = for (
      i <- m.keys.toSeq.sorted
    ) yield
      i + " -> " + m(i).map(p => f"${p.##}%08x").mkString(" ")
    lines.mkString("\n")
  }

  // Instruct EmbeddedKafka to run on any available ports
  val config = EmbeddedKafkaConfig(
    kafkaPort = 0,
    zooKeeperPort = 0
  )

  "Packets" should "pass through a running Kafka" in {
    withRunningKafkaOnFoundPort(config) { implicit actualConfig =>

      val topicName = "packets"
      val numPartitions = 10

      val kafkaHostname = "localhost"
      val kafkaPort = actualConfig.kafkaPort
      val kafkaEndpointString = s"$kafkaHostname:${actualConfig.kafkaPort}"

      val cluster = Cluster.bootstrap(List(new InetSocketAddress("localhost", actualConfig.kafkaPort)).asJava)

      createCustomTopic (topic = topicName, partitions = numPartitions)

      // The packet capture file to use
      val uri = getClass.getResource("/http.cap").toURI
      // Only use the first maxRecords records
      val maxRecords = 400

      val configSource: String =
        """
          |akka.kafka.producer {
          |  parallelism = 100
          |  close-timeout = 60s
          |  use-dispatcher = "akka.kafka.default-dispatcher"
          |  kafka-clients {
          |    key.serializer = "org.apache.kafka.common.serialization.IntegerSerializer"
          |    value.serializer = "edu.uw.at.iroberts.wirefugue.kafka.serdes.protobuf.PacketSerde"
          |  }
          |}
          |
          |akka.kafka.consumer {
          |  kafka-clients {
          |    connections.max.idle.ms = 5000
          |  }
          |}
        """.stripMargin

      val config = ConfigFactory.parseString(configSource)

      implicit val system = ActorSystem("stream-producer-system", config)
      implicit val ec = system.dispatcher
      implicit val materializer = ActorMaterializer()

      // Read the packets into a sequence
      val packetsF = PcapSource(uri).runWith(Sink.seq)
      val packets = Await.result(packetsF, 10 seconds)

      def expectedPartitionForPacket(packet: Packet): Int = {
        val keySerializer = new org.apache.kafka.common.serialization.IntegerSerializer

        val keyBytes = keySerializer.serialize(topicName, packet.key.##)

        // This is a copy of the expression used by DefaultPartitioner
        Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions
      }

      val partitionsExpected = packets.groupBy(expectedPartitionForPacket)

      // Set up the producer and stream the packets to it
      val producerSettings: ProducerSettings[Integer, Packet] =
        ProducerSettings(system, new IntegerSerializer, new PacketSerializer)
          .withBootstrapServers(kafkaEndpointString)
          .withProperty("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner")
          .withProperty("value.serializer", "edu.uw.at.iroberts.kafka.serdes.protobuf.PacketSerde")

      val producerF = Source(packets)
          //.alsoTo(Sink.foreach(println))
        .runWith(PacketProducer.plainSink(topicName, producerSettings))

      // Wait for the producer to finish
      Await.ready(producerF, 60 seconds)

      // Set up the consumer group
      val consumerSettings: ConsumerSettings[Integer, Packet] =
        ConsumerSettings(system, new IntegerDeserializer, new PacketDeserializer)
          .withBootstrapServers(kafkaEndpointString)
          .withGroupId("group1")
          .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      val maxPartitions = numPartitions
      val consumerGroup = Consumer.plainPartitionedSource(consumerSettings, Subscriptions.topics(topicName))

      // Run the subsources from the consumer group and build a map of partitions to packet sequences
      val (consumerControl, packetSeqsByPartitionF): (Consumer.Control, Future[Map[Int, Seq[Packet]]]) =
        consumerGroup.map {
          case (topicPartition, source) =>
            // Run each source until stopped and return the messages in a seq
            // tupled with the partition number
            val p: Int = topicPartition.partition
            source
              .mapConcat[Packet] { (cr: ConsumerRecord[Integer, Packet]) => List(cr.value()) }
              .toMat(Sink.seq)(Keep.right)
              .mapMaterializedValue(xs => p -> xs)
              .run() : (Int, Future[Seq[Packet]])
        }
          // Transform (Int, Future[Seq[_]]) to Future[(Int, Seq[_])]
          .mapAsyncUnordered(maxPartitions) { case (p, fs) => fs.map(xs => p -> xs) }
          // Now we have Source[(Int, Seq[_]), Consumer.Control],
            .toMat(Sink.seq)(Keep.both) // mat: (Consumer.Control, Future[Seq[(Int, Seq[Packet])]]
          .mapMaterializedValue[(Consumer.Control, Future[Map[Int, Seq[Packet]]])] {
            case (ctrl, seqF) => (ctrl, seqF.map(_.filter(_._2.nonEmpty).toMap))
          }
          //.toMat(Sink.ignore)(Keep.both)
          .run()

      // Shut down the consumer group after a delay, causing streams to complete
      akka.pattern.after(20 seconds, using = system.scheduler)(consumerControl.shutdown())

      // Consumers should finish and Sink.seq results will complete
      val packetSeqsByPartition = Await.result(packetSeqsByPartitionF, 60 seconds)

      // Just compare the formatted output, allowing for easy visual inspection in
      // the event of a failure
      formatPacketMap(packetSeqsByPartition) shouldEqual formatPacketMap(partitionsExpected)
    }
  }

}
