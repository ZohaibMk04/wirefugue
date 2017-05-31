package edu.uw.at.iroberts.pcap

import java.nio.ByteOrder
import java.nio.ByteOrder.{BIG_ENDIAN, LITTLE_ENDIAN}

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/** Operations to extract from any IndexedSeq[Byte] (which includes
  * akka.util.ByteString), 32- and 16-bit integers, both
  * signed and unsigned, into Scala/Java's (always-signed) integer
  * types. Endianness may be specified implicitly.
  *
  * Overloaded object method toBytesBE() is provided to convert
  * Int and Short values to Array[Byte] in network order.
  *
  * Also, an IndexedSeq[Byte] can be formatted into various
  * printable strings, notably a multiline display with offsets
  * similar to that produced by `tcpdump -x`.
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  */

object ByteSeqOps {
  implicit def toByteSeqOps[A <: IndexedSeq[Byte]](bytes: A): ByteSeqOps[A] =
    new ByteSeqOps[A](bytes)

  def unsignedIntToSignedLong(u32: Int): Long = u32.toLong & 0xffffffff
  def unsignedShortToSignedInt(u16: Short): Int = u16.toInt & 0xffff
  def unsignedByteToSignedShort(u8: Byte): Short = (u8.toInt & 0xff).toShort

  def toBytesBE(i32: Int): Array[Byte] = {
    val buf: mutable.ArrayBuffer[Byte] = ArrayBuffer()
    buf += (i32 >>> 24).toByte
    buf += (i32 >>> 16).toByte
    buf += (i32 >>> 8).toByte
    buf += i32.toByte
    buf.toArray
  }

  def toBytesBE(i16: Short): Array[Byte] = {
    val buf: mutable.ArrayBuffer[Byte] = ArrayBuffer()
    buf += (i16 >>> 8).toByte
    buf += i16.toByte
    buf.toArray
  }
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

  def getUInt32(implicit byteOrder: ByteOrder): Long = {
    byteOrder match {
      case BIG_ENDIAN => this.getUInt32BE
      case LITTLE_ENDIAN => this.getUInt32LE
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

  def getInt32(implicit byteOrder: ByteOrder): Int = {
    byteOrder match {
      case LITTLE_ENDIAN => getInt32LE
      case BIG_ENDIAN => getInt32BE
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

  def getUInt16(implicit byteOrder: ByteOrder): Int = {
    byteOrder match {
      case LITTLE_ENDIAN => getUInt16LE
      case BIG_ENDIAN => getUInt16BE
    }
  }

  def getUInt16LE: Int = ((bytes(1).toInt & 0xff) << 8) | (bytes(0).toInt & 0xff)
  def getUInt16BE: Int = ((bytes(0).toInt & 0xff) << 8) | (bytes(1).toInt & 0xff)

  def getInt16(implicit byteOrder: ByteOrder): Short = {
    byteOrder match {
      case LITTLE_ENDIAN => getInt16LE
      case BIG_ENDIAN => getInt16BE
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
