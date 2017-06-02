package edu.uw.at.iroberts.pcap.overlay

import java.nio.ByteOrder

import edu.uw.at.iroberts.pcap.ByteSeqOps._
import edu.uw.at.iroberts.pcap.{IPAddress, Protocol}

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
object IPV4Datagram {
  object Flag {
    val MF: Int = 0x01
    val DF: Int = 0x02
    val RESERVED: Int = 0x04
  }
}

case class IPV4Datagram(bytes: IndexedSeq[Byte]) extends Overlay {

  require(bytes.length >= 20)

  case class Flags(asInt: Int) {
    import IPV4Datagram.Flag._
    def mf: Boolean = (asInt & MF) != 0
    def df: Boolean = (asInt & DF) != 0
    def reserved: Boolean = (asInt & RESERVED) != 0
  }

  private implicit val byteOrder = ByteOrder.BIG_ENDIAN

  def ihl: Int = (bytes(0) & 0x0f).toByte
  def version: Byte = ((bytes(0) & 0xf0) >>> 4).toByte
  def dscpAndEcn: Byte = bytes(1)
  def totalLength: Short = bytes.drop(2).getInt16 // UNSIGNED
  def identification: Short = bytes.slice(4, 6).getInt16
  def flags: Flags = Flags(bytes(7) >>> 5)
  def flagMF: Boolean = flags.mf
  def flagDF: Boolean = flags.df
  def offset: Short = (bytes.slice(6, 8).getInt16 & 0x1fff).toShort // UNSIGNED
  def ttl: Byte = bytes(8) // UNSIGNED
  def protocol: Byte = bytes(9)
  def checksum: Short = bytes.slice(10, 12).getInt16
  def src = IPAddress(bytes.slice(12, 16))
  def dest = IPAddress(bytes.slice(16, 20))
  def options: IndexedSeq[Byte] = bytes.slice(20, ihl * 4)
  def data: IndexedSeq[Byte] = bytes.slice(ihl * 4, bytes.length)

  override def toString = {

    val protoStr = f"proto ${Protocol.fromByte(protocol).name} (0x$protocol%02x)"
    val ttlSigned = ttl.toInt & 0xff

    s"$src > $dest, IPv$version length $totalLength, ttl $ttlSigned, $protoStr [${data.length} bytes]"
  }
}

