package edu.uw.at.iroberts.wirefugue.pcap

import java.time.Instant

import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw.LinkType
import edu.uw.at.iroberts.wirefugue.protocol.overlay.{Ethernet, IPV4Datagram, TCPSegment}
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

}

object Packet {
  import edu.uw.at.iroberts.wirefugue.Timestamped

  /** Packet is an instance of Timestamped */
  implicit object PacketsAreTimestamped extends Timestamped[Packet] {
    def timestamp(p: Packet) = p.timestamp
  }
}

