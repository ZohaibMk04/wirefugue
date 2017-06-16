package edu.uw.at.iroberts.wirefugue.pcap

import akka.stream.scaladsl.Flow
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw.LinkType

/** A hacky way to print packets.
  * Works best iff:
  *   1. linkType is Ethernet
  *   2. only IP packets are present
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  */
object PacketLineFormatter {
  def apply() = Flow.fromFunction { p: Packet =>
    val length: Long = p.data.length
    val rcvdLength: Long = p.originalLength
    val maybeFrame = p.network match {
      case LinkType.ETHERNET =>
        if (p.data.length >= EthernetFrame.headerLength)
          Some(EthernetFrame.parse(p.data))
        else
          None
      case _ => None
    }

    val maybePacket = maybeFrame.flatMap { frame =>
      if (frame.etherType == EtherType.IPv4 &&
          frame.payload.length >= Datagram.headerLength)
        Some(Datagram.parse(frame.payload))
      else
        None
    }
    s"[${p.timestamp}] $length bytes" + {
      if (length < rcvdLength) s" (truncated from $rcvdLength)"
      else ""
    } +
      maybeFrame.map("\n" + _).getOrElse("") +
      maybePacket.map("\n" + _).getOrElse("")
  }

}
