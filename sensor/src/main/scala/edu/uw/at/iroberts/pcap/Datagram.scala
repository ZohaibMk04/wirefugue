package edu.uw.at.iroberts.pcap

import akka.util.ByteString

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/5/17.
  */





/** IP protocol field name/value pairs */
abstract class Protocol(val name: String, val value: Byte)

object Protocol {
  case object ICMP extends Protocol("ICMP", 0x01)
  case object TCP extends Protocol("TCP", 0x06)
  case object UDP extends Protocol("UDP", 0x11)
  case class Other(override val value: Byte) extends Protocol("Unknown", value)

  def fromByte(b: Byte): Protocol = b match {
    case 0x01 => ICMP
    case 0x06 => TCP
    case 0x11 => UDP
    case _ => Other(b)
  }
}

/** Operations for constructing Datagram objects */
object Datagram {
  val headerLength = 20
  def parse(bytes: IndexedSeq[Byte]): Datagram = {
    require(bytes.length >= headerLength)
    import ByteSeqOps._

    // IHL (Internet Header Length) is the number of
    // 32-bit words in the IP header.
    val ihl = bytes(0) & 0x0f

    Datagram(
      version = ((bytes(0) & 0xf0) >>> 4).toByte,
      totalLength = bytes.drop(2).getUInt16BE,
      ttl = bytes(8),
      protocol = Protocol.fromByte(bytes(9)),
      sourceIP = IPAddress(bytes.slice(12, 16)),
      destinationIP = IPAddress(bytes.slice(16, 20)),
      data = ByteString(bytes.drop(ihl * 4): _*)
    )
  }

  val fromFrame: PartialFunction[EthernetFrame, Datagram] = {
    case frame: EthernetFrame
      if frame.etherType == EtherType.IPv4 =>
      parse(frame.payload)
  }
}

/** Representation of an IPv4 packet a.k.a. datagram
  *
  * @param version        The IP version
  * @param totalLength    The length including payload data
  * @param ttl            Time-to-live; interpret as unsigned
  * @param protocol       The encapsulated protocol
  * @param sourceIP       Source IP Address
  * @param destinationIP  Destination IP Address
  * @param data           Payload bytes
  */
// TODO: add more fields
case class Datagram private (
                              version: Byte,
                              totalLength: Int,
                              ttl: Byte,    // NB: Unsigned!
                              protocol: Protocol,
                              sourceIP: IPAddress,
                              destinationIP: IPAddress,
                              data: ByteString
                            ) {
  override def toString() = {

    val protoStr = f"proto ${protocol.name} (0x${protocol.value}%02x)"
    val ttlSigned = ttl.toInt & 0xff

    s"$sourceIP > $destinationIP, IPv$version length $totalLength, ttl $ttlSigned, $protoStr [${data.length} bytes]"
  }

}
