package edu.uw.at.iroberts.wirefugue.kafka.producer

import edu.uw.at.iroberts.wirefugue.pcap.{IPAddress, Protocol}
import edu.uw.at.iroberts.wirefugue.protocol.overlay.{IPV4Datagram, TCPSegment, UDPDatagram}
import kafka.scala.SimplePartitioner
import org.apache.kafka.common.Cluster

import edu.uw.at.iroberts.wirefugue.pcap.ByteSeqOps._

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

    protocol * p1 +
      (src.bytes.getInt32BE * p2 + sport * p3) +
      (dest.bytes.getInt32BE * p2 + dport * p3)
  }
}

class IPv4Partitioner extends SimplePartitioner with DefaultPacketHashStrategy {

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

    partitions.get((hash & Integer.MAX_VALUE) % nPartitions).partition()
  }

}
