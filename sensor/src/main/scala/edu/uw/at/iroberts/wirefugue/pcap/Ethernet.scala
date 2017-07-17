package edu.uw.at.iroberts.wirefugue.pcap

import java.nio.ByteOrder

import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.protocol.overlay.EtherType




case class EthernetFrame(
                          destinationMAC: MACAddress,
                          sourceMAC: MACAddress,
                          etherType: EtherType.Value,
                          payload: ByteString
                        ) {
  override def toString = {
    val len = payload.length

    f"$sourceMAC > $destinationMAC, ethertype $etherType 0x${etherType.id}%04x, length $len"
  }
}

object EthernetFrame {
  val headerLength = 14
}

