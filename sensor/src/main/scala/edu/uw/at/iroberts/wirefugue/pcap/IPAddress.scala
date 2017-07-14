package edu.uw.at.iroberts.wirefugue.pcap

import java.net.{Inet4Address, InetAddress}

/** Represents an Internet Protocol version 4 address
  *
  * @param inet4Address a java.net.Inet4Address
  */
case class IPAddress(inet4Address: Inet4Address) {

  def bytes: IndexedSeq[Byte] = inet4Address.getAddress.toIndexedSeq

  override def toString = inet4Address.getHostAddress
}

object IPAddress {

  def apply(s: String): IPAddress = {
    val ints = s.split("\\.").map(Integer.parseInt)
    require( ints.forall(i => i >= 0 && i <= 255) )
    this(ints.map(_.toByte))
  }

  def apply(bytes: Seq[Byte]): IPAddress = {
    require(bytes.length == 4)
    IPAddress(InetAddress.getByAddress(bytes.toArray).asInstanceOf[Inet4Address])
  }
}