package edu.uw.at.iroberts.wirefugue.pcap

import akka.util.ByteString

/**
  * Created by scala on 5/18/17.
  */
object InternetChecksum {
  def onesSum(data: IndexedSeq[Byte]): Short = {
    import ByteSeqOps._

    // Try to be somewhat efficient here. Summing 32-bit words
    // into a 64-bit accumulator would probably be better
    // for 64-bit architectures.
    val length = data.length
    val mod2 = length % 2
    var sum: Int = data
      .slice(0, length - mod2)
      .grouped(2)
      .map(_.getUInt16BE)
      .sum
    if (mod2 != 0)
      sum += data.last << 8

    ((sum & 0xffff) + (sum >>> 16)).toShort
  }

  // Results in the one's complement of onesSum(data) as a 16-bit integer,
  // which is the value that should be placed in the checksum field.
  def internetChecksum(data: IndexedSeq[Byte]): Short = (~onesSum(data)).toShort
}

object InternetChecksumDemo extends App {
  import InternetChecksum._
  val data: IndexedSeq[Byte] = ByteString.fromInts(
    0x00, 0x01, 0xf2, 0x03,
    0xf4, 0xf5, 0xf6, 0xf7
  )

  val sum = internetChecksum(data)
  val ones = onesSum(data)
  println(f"sum = 0x$sum%04x")
  println(f"ones = 0x$ones%04x")
}
