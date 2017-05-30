package edu.uw.at.iroberts.pcap.overlay

import edu.uw.at.iroberts.pcap.ByteSeqOps._
import edu.uw.at.iroberts.pcap.Endianness
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
case class TCPSegment(bytes: IndexedSeq[Byte]) {
  object TcpFlagMask extends Enumeration {
    val FIN = Value(0x1)
    val SYN = Value(0x2)
    val RST = Value(0x4)
    val PSH = Value(0x8)
    val ACK = Value(0x10)
    val URG = Value(0x20)
    val ECE = Value(0x40)
    val CWR = Value(0x80)
    val NS = Value(0x100)
  }
  import TcpFlagMask._
  implicit val endianness = Endianness.Big
  def sport: Short = bytes.slice(0, 2).getInt16
  def dport: Short = bytes.slice(2, 4).getInt16
  def seqN: Int = bytes.slice(4, 8).getInt32 // UNSIGNED
  def ackN: Int = bytes.slice(8, 12).getInt32 // UNSIGNED
  def flags: Int = bytes.slice(12, 14).getInt16 & 0x1f
  def synFlag: Boolean = (flags & SYN.id) != 0
  def finFlag: Boolean = (flags & FIN.id) != 0
  def rstFlag: Boolean = (flags & RST.id) != 0
  def ackFlag: Boolean = (flags & ACK.id) != 0
  def pshFlag: Boolean = (flags & PSH.id) != 0
  // TODO: add more fields
}
