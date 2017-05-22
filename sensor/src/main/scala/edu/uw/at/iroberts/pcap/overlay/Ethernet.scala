package edu.uw.at.iroberts.pcap.overlay

import edu.uw.at.iroberts.pcap.ByteSeqOps._
import edu.uw.at.iroberts.pcap._

case class Ethernet(bytes: IndexedSeq[Byte]) extends Overlay {
  require(bytes.length >= 14)
  def src = MACAddress(bytes.slice(0,6))
  def dst = MACAddress(bytes.slice(6,12))
  def etherType: Short = bytes.slice(12, 14).getInt16BE
  def payload = bytes.drop(14)
}



