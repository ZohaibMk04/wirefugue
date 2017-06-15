package edu.uw.at.iroberts.pcap.kafkaproducer

import edu.uw.at.iroberts.pcap.{IPAddress, Protocol}
import edu.uw.at.iroberts.pcap.overlay.{IPV4Datagram, TCPSegment, UDPDatagram}
import kafka.scala.SimplePartitioner
import org.apache.kafka.common.Cluster

import edu.uw.at.iroberts.pcap.ByteSeqOps._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/15/17.
  */

trait PacketHashStrategy {
  def hashPacket(protocol: Byte,
                 sourceAddress: IPAddress,
                 destinationAddress: IPAddress,
                 sourcePort: Short,
                 destPort: Short): Int

}

trait DefaultPacketHashStrategy extends PacketHashStrategy {
  def hashPacket(protocol: Byte, src: IPAddress, dest: IPAddress, sport: Short, dport: Short): Int = {
    val p1 = 2 ^ 13 - 1
    val p2 = 2 ^ 17 - 1
    val p3 = 2 ^ 19 - 1
    // FIXME: This hash algorithm collides on unrelated flows
    // when source and destination ports are swapped, e.g.:
    // 10.0.0.1:12345 -> 192.168.0.1:80
    // and
    // 10.0.0.1:80 -> 192.168.0.1:12345
    // hash to the same value. In practice this seems unlikely
    // to produce many collisions.
    protocol * p1 +
      (src.bytes.getInt32BE ^ dest.bytes.getInt32BE) * p2 +
      (sport ^ dport) * p3
  }
}

object IPv4Partitioner extends SimplePartitioner with DefaultPacketHashStrategy {

  def partition(topic: String,
                      key: Option[Any],
                      keyBytes: Option[IndexedSeq[Byte]],
                      value: Option[Any],
                      valueBytes: Option[IndexedSeq[Byte]],
                      cluster: Cluster): Int = {
    require(valueBytes.isDefined)

    val partitions = cluster.availablePartitionsForTopic(topic)
    val nPartitions = partitions.size()

    val dg: IPV4Datagram = IPV4Datagram(valueBytes.get)

    val sport =
      if (dg.offset != 0) 0 // can't read the port from sequel fragments
      else
        dg.protocol match {
          case Protocol.TCP.value => TCPSegment(dg.data).sport
          case Protocol.UDP.value => UDPDatagram(dg.data).sport
          case _ => 0
        }

    val dport = if (dg.offset != 0) 0
    else
      dg.protocol match {
        case Protocol.TCP.value => TCPSegment(dg.data).dport
        case Protocol.UDP.value => UDPDatagram(dg.data).dport
        case _ => 0
      }

    val hash = hashPacket(dg.protocol, dg.src, dg.dest, sport.toShort, dport.toShort)

    partitions.get(hash % nPartitions).partition()
  }

}
