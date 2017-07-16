package edu.uw.at.iroberts.wirefugue.kafka.producer

import akka.Done
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import edu.uw.at.iroberts.wirefugue.pcap.Packet
import edu.uw.at.iroberts.wirefugue.protobufs.packet.{Packet => ProtobufPacket}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Future

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/1/17.
  */
object PacketProducer {
  def plainSink(topic: String, producerSettings: ProducerSettings[Integer, ProtobufPacket]): Sink[Packet, Future[Done]] =
  Flow[Packet].map( p => new ProducerRecord[Integer, ProtobufPacket](topic, p.key.##, packetToProtobufPacket(p)))
    .toMat(Producer.plainSink(producerSettings))(Keep.right)
}
