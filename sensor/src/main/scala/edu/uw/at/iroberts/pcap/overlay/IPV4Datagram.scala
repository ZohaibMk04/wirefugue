package edu.uw.at.iroberts.pcap.overlay

import edu.uw.at.iroberts.pcap.{IPAddress, Protocol}
import edu.uw.at.iroberts.pcap.ByteSeqOps._

import scala.collection.SeqView

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
case class IPV4Datagram[+Repr <: IndexedSeq[Byte]](bytes: Repr) extends Overlay {

  require(bytes.length >= 20)

  def ihl: Int = (bytes(0) & 0x0f).toByte
  def version: Byte = ((bytes(0) & 0xf0) >>> 4).toByte
  def totalLength: Short = bytes.drop(2).getInt16BE // UNSIGNED
  def ttl: Byte = bytes(8) // UNSIGNED
  def protocol: Byte = bytes(9)
  def src = IPAddress(bytes.slice(12, 16))
  def dest = IPAddress(bytes.slice(16, 20))
  def data: SeqView[Byte, IndexedSeq[Byte]] = bytes.view(ihl * 4, bytes.length)

  override def toString = {

    val protoStr = f"proto ${Protocol.fromByte(protocol).name} (0x$protocol%02x)"
    val ttlSigned = ttl.toInt & 0xff

    s"$src > $dest, IPv$version length $totalLength, ttl $ttlSigned, $protoStr [${data.length} bytes]"
  }
}

object IPV4DatagramPartial extends Overlay {
  def version: PartialFunction[IndexedSeq[Byte], Byte] = new PartialFunction[IndexedSeq[Byte], Byte] {
    override def isDefinedAt(bytes: IndexedSeq[Byte]): Boolean = bytes.nonEmpty
    override def apply(bytes: IndexedSeq[Byte]): Byte = bytes(0)
  }
  def version2: PartialFunction[IndexedSeq[Byte], Byte] = { case x if x.nonEmpty => ((x(0) & 0xf0) >> 4).toByte }
  def ihl: PartialFunction[IndexedSeq[Byte], Byte] = { case x: IndexedSeq[Byte] if x.nonEmpty => (x(0) & 0x0f).toByte}
  def data: PartialFunction[IndexedSeq[Byte], SeqView[Byte, IndexedSeq[Byte]]] = { case x: IndexedSeq[Byte] if x.length >= (ihl(x) * 4) => x.view(ihl(x) * 4, x.length) }


}

case class IPV4DatagramPartial(bytes: IndexedSeq[Byte]) extends Overlay {
  // How to handle failures?:

  // PartialFunction
  def version: PartialFunction[Unit, Byte] = new PartialFunction[Unit, Byte] {
    override def isDefinedAt(x: Unit): Boolean = bytes.length >= 1
    override def apply(x: Unit): Byte = bytes(0)
  }

  // More succinct PartialFunction
  def version2: PartialFunction[IndexedSeq[Byte], Byte] = { case x: IndexedSeq[Byte] if x.nonEmpty => x(0) }

  // Option
  def totalLength: Option[Short] = Some(bytes)
    .filter(_.length >= 3)
    .map(_.drop(2))
    .map({bs: IndexedSeq[Byte] => bs.getInt16BE})

  // Try
  import scala.util.{Failure, Success, Try}
  def ttl: Try[Byte]= {
    if (bytes.length >= 8)
      Success(bytes(8))
    else
      Failure(new RuntimeException("I failed you."))
  }

  // Throw a IndexOutOfBounds
  def protocol: Byte = bytes(9)
  def src = IPAddress(bytes.slice(12, 16))
  def dest = IPAddress(bytes.slice(16, 20))
  def data: SeqView[Byte, IndexedSeq[Byte]] = {
    val ihl: Int = (bytes(0) & 0x0f).toByte
    bytes.view(ihl * 4, bytes.length)
  }
}

