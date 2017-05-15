package edu.uw.at.iroberts.pcap

import akka.util.ByteString


object EtherType extends Enumeration {
  val IPv4 = Value(0x0800)
  val IPv6 = Value(0x86dd)
  val ARP = Value(0x0806)
  // TODO: Add more EtherType values
  def other(i: Int): EtherType.Value = Value(i)
}

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
  def parse(bytes: ByteString): EthernetFrame = {
    require(bytes.length >= headerLength)
    import ByteSeqOps._
    implicit val endianness = Endianness.Big

    EthernetFrame(
      destinationMAC = MACAddress(bytes.slice(0, 6)),
      sourceMAC = MACAddress(bytes.slice(6, 12)),
      etherType = EtherType(bytes.slice(12, 14).getUInt16),
      payload = bytes.drop(14)
    )
  }

}

