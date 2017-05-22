package edu.uw.at.iroberts.pcap.overlay

import edu.uw.at.iroberts.pcap.ByteSeqOps._
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
case class TCPSegment(bytes: IndexedSeq[Byte]) {
  def sport: Short = bytes.slice(0, 2).getInt16BE
  def dport: Short = bytes.slice(2, 4).getInt16BE
  // TODO: add more fields
}
