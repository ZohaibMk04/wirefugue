package edu.uw.at.iroberts.pcap.overlay

import edu.uw.at.iroberts.pcap.{IPAddress, Protocol}
import edu.uw.at.iroberts.pcap.ByteSeqOps._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
case class IPV4Datagram(bytes: IndexedSeq[Byte]) extends Overlay {

  require(bytes.length >= 20)

  def ihl: Int = (bytes(0) & 0x0f).toByte
  def version: Byte = ((bytes(0) & 0xf0) >>> 4).toByte
  def totalLength: Short = bytes.drop(2).getInt16BE // UNSIGNED
  def ttl: Byte = bytes(8) // UNSIGNED
  def protocol: Byte = bytes(9)
  def src = IPAddress(bytes.slice(12, 16))
  def dest = IPAddress(bytes.slice(16, 20))
  def data: IndexedSeq[Byte] = bytes.slice(ihl * 4, bytes.length)

  override def toString = {

    val protoStr = f"proto ${Protocol.fromByte(protocol).name} (0x$protocol%02x)"
    val ttlSigned = ttl.toInt & 0xff

    s"$src > $dest, IPv$version length $totalLength, ttl $ttlSigned, $protoStr [${data.length} bytes]"
  }
}

