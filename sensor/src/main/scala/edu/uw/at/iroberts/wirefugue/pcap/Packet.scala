package edu.uw.at.iroberts.wirefugue.pcap

import java.time.Instant

import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.kafka.serdes.PacketKey
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw.LinkType
import edu.uw.at.iroberts.wirefugue.protocol.overlay.{EtherType, Ethernet, IPV4Datagram, TCPSegment}
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/9/17.
  */

/** Packet represents
  * all we care to know about a single pcap file record,
  * regardless of which file it came from.
  * The timestamp is always in UTC with nanosecond
  * precision and the link-layer header type from the pcap
  * file header is included. includedLength is, well, not included
  * because that information can be obtained (in constant time)
  * from data.length.
  */
case class Packet(
                    timestamp: Instant,
                    network: LinkType.Value,
                    originalLength: Int,
                    data: ByteString
                 ) {
  def ip: Option[IPV4Datagram] = this match {
    case Packet(_, LinkType.ETHERNET, _, eth)
      if eth.length >= Ethernet.minSize && Ethernet(eth).etherType == EtherType.IPv4.id =>
      Some(IPV4Datagram(Ethernet(eth).payload))
    case _ => None
  }

  def tcp: Option[TCPSegment] = for {
    ip <- this.ip
    if ip.protocol == Protocol.TCP.value && ip.data.length >= TCPSegment.minSize
  } yield TCPSegment(ip.data)

  def key: PacketKey = PacketKey(
    protocol = ip.map(_.protocol).getOrElse(0),
    sourceIP = ip.map(_.src).getOrElse(IPAddress("0.0.0.0")),
    sourcePort = tcp.map(_.sport).getOrElse(0),
    destinationIP = ip.map(_.dest).getOrElse(IPAddress("0.0.0.0")),
    destinationPort = tcp.map(_.dport).getOrElse(0)
  )

  override def toString = this match {
    case Packet(ts, LinkType.ETHERNET, _, eth) =>
      ts.toString + " " +
        ip.map(_.toString).getOrElse("") + " " +
        tcp.map(_.toString).getOrElse("")
    case _ => "[Non-ethernet packet]"
  }
}

object Packet {
  import edu.uw.at.iroberts.wirefugue.Timestamped

  /** Packet is an instance of Timestamped */
  implicit object PacketsAreTimestamped extends Timestamped[Packet] {
    def timestamp(p: Packet) = p.timestamp
  }
}

