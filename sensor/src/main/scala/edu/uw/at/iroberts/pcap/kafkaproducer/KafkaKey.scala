package edu.uw.at.iroberts.pcap.kafkaproducer

import edu.uw.at.iroberts.pcap.Protocol
import edu.uw.at.iroberts.pcap.overlay.{IPV4Datagram, TCPSegment, UDPDatagram}
import edu.uw.at.iroberts.pcap.ByteSeqOps._

import org.apache.kafka.common.serialization.Serializer

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
    KafkaKey(packet.protocol * p1 +
      (packet.src.bytes.getInt32BE ^ packet.dest.bytes.getInt32BE) * p2 +
      (sport ^ dport) * p3)
  }

}
