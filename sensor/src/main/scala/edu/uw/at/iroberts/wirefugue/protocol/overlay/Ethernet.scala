package edu.uw.at.iroberts.wirefugue.protocol.overlay

import edu.uw.at.iroberts.wirefugue.pcap.ByteSeqOps._
import edu.uw.at.iroberts.wirefugue.pcap._

case class Ethernet(bytes: IndexedSeq[Byte]) extends Overlay {
  require(bytes.length >= Ethernet.minSize)
  def src = MACAddress(bytes.slice(0,6))
  def dst = MACAddress(bytes.slice(6,12))
  def etherType: Short = bytes.slice(12, 14).getInt16BE
  def payload = bytes.drop(14)
}

object Ethernet {
  val minSize = 14
}



