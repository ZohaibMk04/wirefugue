package edu.uw.at.iroberts.pcap

import akka.util.ByteString
import org.apache.kafka.common.serialization.Serializer

import scala.collection.mutable.ArrayBuffer

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
      ihl = ihl.toByte,
      dscpAndECN = bytes(1),
      totalLength = bytes.drop(2).getInt16BE,
      identification = bytes.slice(4, 6).getInt16BE,
      flags = ((bytes(6) & 0xff) >> 5).toByte,
      fragmentOffset = (bytes.slice(6, 8).getUInt16BE & 0x1fff).toShort,
      ttl = bytes(8),
      protocol = Protocol.fromByte(bytes(9)),
      headerChecksum = bytes.slice(10, 12).getInt16BE,
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
                              version: Byte = 0x04.toByte,
                              ihl: Byte = 0x05.toByte,
                              dscpAndECN: Byte = 0x00.toByte,
                              totalLength: Short, // NB: Unsigned!
                              identification: Short = 0x0000.toShort,
                              flags: Byte = 0x00.toByte,
                              fragmentOffset: Short = 0x0000.toShort,
                              ttl: Byte = 128.toByte,    // NB: Unsigned!
                              protocol: Protocol = Protocol.ICMP,
                              headerChecksum: Short = 0x0000.toShort,
                              sourceIP: IPAddress = IPAddress(IndexedSeq[Byte](0, 0, 0, 0)),
                              destinationIP: IPAddress = IPAddress(IndexedSeq[Byte](0, 0, 0, 0)),
                              data: ByteString = ByteString()
                            ) {
  override def toString() = {

    val protoStr = f"proto ${protocol.name} (0x${protocol.value}%02x)"
    val ttlSigned = ttl.toInt & 0xff

    s"$sourceIP > $destinationIP, IPv$version length $totalLength, ttl $ttlSigned, $protoStr [${data.length} bytes]"
  }

  def toBytes: Array[Byte] = {
    def serializeShort(x: Short): Array[Byte] = Array(
      ((x >> 8) & 0xff).toByte,
      (x & 0xff).toByte
    )
    val buf: ArrayBuffer[Byte] = new ArrayBuffer[Byte](ihl * 4)

    val versionAndIHL: Byte = (((version & 0x0f) << 4) | (ihl & 0x0f)).toByte
    val flagsAndFragmentOffset: Short = (((flags & 0x07) << 13) | (fragmentOffset & 0x1fff)).toShort

    buf += versionAndIHL
    buf += dscpAndECN
    buf ++= serializeShort(totalLength)
    buf ++= serializeShort(identification)
    buf ++= serializeShort(flagsAndFragmentOffset)
    buf += ttl
    buf += protocol.value
    buf ++= serializeShort(headerChecksum)
    buf ++= sourceIP.bytes
    buf ++= destinationIP.bytes

    buf ++= data

    buf.toArray
  }

  def mkDatagram(
                version: Int,
                dspAndEcn: Int,
                identification: Int,
                flags: Int,
                fragmentOffset: Int,
                ttl: Int,
                protocol: Int,
                headerChecksum: Int,
                sourceIP: Int,
                destinationIP: Int,
                data: TraversableOnce[Byte]
                ): Datagram = {
    val ihl = 5
    val fragmentOffset = 0
    val checksum = 0

    require(version >= 0x0 && version <= 0xf)

    val myData = data.toArray

    Datagram(totalLength = 0)

  }

  import edu.uw.at.iroberts.pcap.InternetChecksum._
  def fixChecksum: Datagram = {
    val dg = copy(headerChecksum = 0.toShort)
    copy(headerChecksum = internetChecksum(dg.toBytes.slice(0, ihl*4)))
  }

  def checksumValid: Boolean = {
    internetChecksum(toBytes.slice(0, ihl*4)) == 0
  }


}

case object DatagramSerializer extends Serializer[Datagram] {
  override def configure(configs: java.util.Map[String, _], isKey: Boolean) = {
    /* Nothing to do */
  }

  override def close() = { /* Nothing to do */ }

  override def serialize(topic: String, data: Datagram): Array[Byte] =
    data.toBytes
}

object DatagramSerializerDemo extends App {
  val ser = DatagramSerializer

  val dg =
    Datagram(
      version = 4.toByte,
      ihl = 5.toByte,
      dscpAndECN = 0.toByte,
      totalLength = 28.toShort,
      identification = 1234.toShort,
      flags = 0x00.toByte,
      fragmentOffset = 0,
      ttl = 128.toByte,
      protocol = Protocol.ICMP,
      headerChecksum = 0x0000,
      sourceIP = IPAddress("192.168.0.1"),
      destinationIP = IPAddress("172.16.2.16"),
      data = ByteString(0x01, 0x00, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78)
    ).fixChecksum
  val data: Array[Byte] = ser.serialize("ip-packets", dg)
  val output = data.grouped(2).map { case Array(x, y) => f"$x%02x$y%02x" }.mkString(" ")
  println(output)
  println(dg.checksumValid)
}

