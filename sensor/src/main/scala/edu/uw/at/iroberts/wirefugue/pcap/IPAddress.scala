package edu.uw.at.iroberts.wirefugue.pcap

import akka.util.ByteString

/** Represents a 4-byte IPv4 address
  *
  * @param bytes
  */
case class IPAddress(bytes: IndexedSeq[Byte]) {
  require(bytes.length == 4)
  override def toString = {
    bytes.map(_.toInt & 0xff).mkString(".")
  }
}

object IPAddress {
  /* Warning: this does no checking to make sure the string
   * represents a sane IP address. In particular values outside
   * of 0-255 are incorporated modulo 256. It is intended only
   * to help make tests easier to write.
   */
  def apply(s: String): IPAddress = this(
    ByteString(s.split("\\.").map(Integer.parseInt): _*)
  )
}