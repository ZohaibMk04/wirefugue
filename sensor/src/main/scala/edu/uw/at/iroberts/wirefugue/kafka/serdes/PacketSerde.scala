package edu.uw.at.iroberts.wirefugue.kafka.serdes

import java.time.Instant

import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.kafka.producer._
import edu.uw.at.iroberts.wirefugue.pcap.{Packet, PcapFileRaw}
import org.apache.kafka.common.serialization._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/1/17.
  */
class PacketSerializer extends Serializer[Packet] with StatelessSerializer[Packet] {
  override def serialize(topic: String, p: Packet): Array[Byte] = {
    packetToProtobufPacket(p).toByteArray
  }
}

class PacketDeserializer extends Deserializer[Packet] with StatelessDeserializer[Packet] {
  override def deserialize(topic: String, data: Array[Byte]): Packet = {
    import edu.uw.at.iroberts.wirefugue.protobufs.packet.{Packet => ProtoPacket}
    val pp = ProtoPacket.parseFrom(data)
    Packet(
      timestamp = pp.sensorTsEpochNanos match {
        case Some(nanos) => Instant.ofEpochSecond(0, nanos)
        case _ => throw new RuntimeException("Packet has no timestamp")
      },
      network = PcapFileRaw.LinkType.ETHERNET, // TODO: don't assume link type
      originalLength = pp.data match {
        case Some(bytes) => bytes.size
        case _ => throw new RuntimeException("Packet has no data")
      },
      data = pp.data match {
        case Some(bytes) => ByteString(bytes.toByteArray)
        case _ => throw new RuntimeException("Packet has no data")
      }
    )
  }
}

class PacketSerde extends Serde[Packet] with StatelessSerde[Packet] {
  val protoPacketSerde = new protobuf.PacketSerde

  override def serializer = new PacketSerializer

  override def deserializer = new PacketDeserializer

}
