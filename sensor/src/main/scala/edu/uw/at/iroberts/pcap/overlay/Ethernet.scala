package edu.uw.at.iroberts.pcap.overlay

import edu.uw.at.iroberts.pcap.ByteSeqOps._
import edu.uw.at.iroberts.pcap._

import scala.collection.SeqView

/**
  * Created by scala on 5/6/17.
  */
trait Overlay

case class Ethernet(bytes: IndexedSeq[Byte]) extends Overlay {
  require(bytes.length >= 14)
  def src = MACAddress(bytes.slice(0,6))
  def dst = MACAddress(bytes.slice(6,12))
  def etherType: Short = bytes.slice(12, 14).getInt16BE
  def payload = bytes.drop(14)
}

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
  def totalLength: Option[Short] = Some(bytes).filter(_.length >= 3).map(_.drop(2)).map({bs: IndexedSeq[Byte] => bs.getInt16BE})

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

object OverlayDemo extends App {
  import javax.xml.bind.DatatypeConverter

  import akka.util.ByteString
  // val packet0: ByteString = ByteString.fromInts(Array.fill(28)(scala.util.Random.nextInt() % 256): _*)
  val packet: ByteString = ByteString.fromArray(
    DatatypeConverter.parseHexBinary(
      "010203040506" +
      "111213141516" +
      "0004" +
      "2021222324252627282930313233343536373839"
    )
  )

  val ethernet = Ethernet(packet)
  println(ethernet.src)
  println(ethernet.dst)
  println(ethernet.etherType)
  println(ethernet.payload)
  ethernet.etherType match {
    case 0x0004 =>
      println(s"length: ${ethernet.payload.length}")
      val ip4 = IPV4Datagram(ethernet.payload)
      println(ip4.src)
      println(ip4.dest)
    case x =>
      println(f"Expected 0x0004, got 0x$x%04x")
  }

}

