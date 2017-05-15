package edu.uw.at.iroberts.pcap

import scala.collection.generic.CanBuildFrom


/** Operations to extract from any IndexedSeq[Byte] (which includes
  * akka.util.ByteString), 32- and 16-bit integers, both
  * signed and unsigned, into Scala/Java's (always-signed) integer
  * types. Endianness may be specified implicitly.
  *
  * Also, an IndexedSeq[Byte] can be formatted into various
  * printable strings, notably a multiline display with offsets
  * similar to that produced by `tcpdump -x`.
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  */

sealed trait Endianness
object Endianness {
  case object Big extends Endianness
  case object Little extends Endianness
}

object ByteSeqOps {
  implicit def toByteSeqOps[A <: IndexedSeq[Byte]](bytes: A): ByteSeqOps[A] =
    new ByteSeqOps[A](bytes)

  def unsignedIntToSignedLong(u32: Int): Long = u32.toLong & 0xffffffff
  def unsignedShortToSignedInt(u16: Short): Int = u16.toInt & 0xffff
  def unsignedByteToSignedShort(u8: Byte): Short = (u8.toInt & 0xff).toShort
}

class ByteSeqOps[A <: IndexedSeq[Byte]](bytes: A) {
  import ByteSeqOps._

  def swab32[B](implicit cbf: CanBuildFrom[A, Byte, B]): B = {
    require(bytes.length >= 4)
    val builder = cbf()
    builder ++= bytes.take(4).reverse
    builder.result()
  }

  def swab16[B](implicit cbf: CanBuildFrom[A, Byte, B]): B = {
    require(bytes.length >= 2)
    val builder = cbf()
    builder ++= bytes.take(2).reverse
    builder.result()
  }

  def getUInt32(implicit endianness: Endianness): Long = {
    import Endianness._
    endianness match {
      case Big => this.getUInt32BE
      case Little => this.getUInt32LE
    }
  }

  def getUInt32LE: Long = {
    require(bytes.length >= 4)
    unsignedIntToSignedLong(getInt32LE)
  }

  def getUInt32BE: Long = {
    require(bytes.length >= 4)
    unsignedIntToSignedLong(getInt32BE)
  }

  def getInt32(implicit endianness: Endianness): Int = {
    import Endianness._
    endianness match {
      case Little => getInt32LE
      case Big => getInt32BE
    }
  }

  def getInt32LE: Int = {
    (bytes(3).toInt << 24) |
      ((bytes(2).toInt & 0xff) << 16) |
      ((bytes(1).toInt & 0xff) << 8) |
      (bytes(0).toInt & 0xff)
  }

  def getInt32BE: Int = {
    (bytes(0).toInt << 24) |
      ((bytes(1).toInt & 0xff) << 16) |
      ((bytes(2).toInt & 0xff) << 8) |
      (bytes(3).toInt & 0xff)
  }

  def getUInt16(implicit endianness: Endianness): Int = {
    import Endianness._
    endianness match {
      case Little => getUInt16LE
      case Big => getUInt16BE
    }
  }

  def getUInt16LE: Int = ((bytes(1).toInt & 0xff) << 8) | (bytes(0).toInt & 0xff)
  def getUInt16BE: Int = ((bytes(0).toInt & 0xff) << 8) | (bytes(1).toInt & 0xff)

  def getInt16(implicit endianness: Endianness): Short = {
    import Endianness._
    endianness match {
      case Little => getInt16LE
      case Big => getInt16BE
    }
  }

  def getInt16LE: Short = ((bytes(1) & 0xff) << 8 | (bytes(0) & 0xff)).toShort
  def getInt16BE: Short = ((bytes(0) & 0xff) << 8 | (bytes(1) & 0xff)).toShort


  def mkHexString: String = bytes.map(b => f"$b%02x").grouped(2).map(_.mkString("")).mkString(" ")
  def mkHexLines: Iterator[String] = {
    bytes.grouped(16).map(_.mkHexString)
  }
  def mkHexBlock(indent: Int = 8): String = {
    mkHexLines.zip(Iterator.from(0, 16))
      .map { case (s, i) => (" " * indent) + f"0x$i%04x:  " + s }
      .mkString("\n")
  }
}
