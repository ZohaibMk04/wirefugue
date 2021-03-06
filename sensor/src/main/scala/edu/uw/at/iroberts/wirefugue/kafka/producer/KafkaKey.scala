package edu.uw.at.iroberts.wirefugue.kafka.producer

import edu.uw.at.iroberts.wirefugue.pcap.ByteSeqOps._
import edu.uw.at.iroberts.wirefugue.pcap.Protocol
import edu.uw.at.iroberts.wirefugue.protocol.overlay.{IPV4Datagram, TCPSegment, UDPDatagram}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
case class KafkaKey(n: Int) {
  def serialize: Array[Byte] = toBytesBE(n)
}

object KafkaKey {
  val serializer = new Serializer[KafkaKey] {
    override def configure(configs: java.util.Map[String, _], isKey: Boolean) = {
      /* Nothing to do */
    }

    override def close() = { /* Nothing to do */ }

    override def serialize(topic: String, key: KafkaKey): Array[Byte] = key.serialize
  }

  val deserializer = new Deserializer[KafkaKey] {
    override def configure(configs: java.util.Map[String, _], isKey: Boolean) = {
      /* Nothing to do */
    }

    override def close() = { /* Nothing to do */ }

    override def deserialize(topic: String, data: Array[Byte]): KafkaKey =
      KafkaKey(wrapByteArray(data).getInt32BE)
  }

  val serde = new Serde[KafkaKey] {
    override def configure(configs: java.util.Map[String, _], isKey: Boolean) = ()
    override val close = ()
    override val serializer = KafkaKey.serializer
    override val deserializer = KafkaKey.deserializer
  }

  def fromIPV4Datagram(packet: IPV4Datagram): KafkaKey = {
    // TODO: Is this a good hashing method? Perform collision
    // analysis on real-world data...
    val sport = packet.protocol match {
      case Protocol.TCP.value => TCPSegment(packet.data).sport
      case Protocol.UDP.value => UDPDatagram(packet.data).sport
      case _ => 0
    }
    val dport = packet.protocol match {
      case Protocol.TCP.value => TCPSegment(packet.data).dport
      case Protocol.UDP.value => UDPDatagram(packet.data).dport
    }
    val p1 = 2^13 - 1
    val p2 = 2^17 - 1
    val p3 = 2^19 - 1
    // FIXME: This hash algorithm collides on unrelated flows
    // when source and destination ports are swapped, e.g.:
    // 10.0.0.1:12345 -> 192.168.0.1:80
    // and
    // 10.0.0.1:80 -> 192.168.0.1:12345
    // hash to the same value. In practice this seems unlikely
    // to produce many collisions.
    KafkaKey(packet.protocol * p1 +
      (packet.src.bytes.getInt32BE ^ packet.dest.bytes.getInt32BE) * p2 +
      (sport ^ dport) * p3)
  }

}
