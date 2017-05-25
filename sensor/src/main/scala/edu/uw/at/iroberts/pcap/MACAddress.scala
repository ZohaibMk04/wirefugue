package edu.uw.at.iroberts.pcap

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/5/17.
  */

case class MACAddress(bytes: IndexedSeq[Byte]) {
  require(bytes.length == 6)
  override def toString = bytes.map(b => f"$b%02x").mkString(":")
}
