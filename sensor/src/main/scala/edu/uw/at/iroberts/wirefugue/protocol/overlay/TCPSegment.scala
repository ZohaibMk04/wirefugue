package edu.uw.at.iroberts.wirefugue.protocol.overlay

import java.nio.ByteOrder

import edu.uw.at.iroberts.wirefugue.pcap.ByteSeqOps._
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
object TCPSegment {
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
}

case class TCPSegment(bytes: IndexedSeq[Byte]) extends Overlay {

  case class TCPFlags(asInt: Int) {
    import TCPSegment.TcpFlagMask._

    def fin: Boolean = (asInt & FIN.id) != 0
    def syn: Boolean = (asInt & SYN.id) != 0
    def rst: Boolean = (asInt & RST.id) != 0
    def psh: Boolean = (asInt & PSH.id) != 0
    def ack: Boolean = (asInt & ACK.id) != 0
    def urg: Boolean = (asInt & URG.id) != 0
    def ece: Boolean = (asInt & ECE.id) != 0
    def cwr: Boolean = (asInt & CWR.id) != 0
    def ns : Boolean = (asInt &  NS.id) != 0
  }

  implicit val byteOrder = ByteOrder.BIG_ENDIAN

  def sport: Short = bytes.slice(0, 2).getInt16
  def dport: Short = bytes.slice(2, 4).getInt16
  def sequenceNumber: Int = bytes.slice(4, 8).getInt32 // UNSIGNED
  def acknowlegementNumber: Int = bytes.slice(8, 12).getInt32 // UNSIGNED
  def dataOffset: Byte = (bytes(12) >>> 4).toByte
  def flags: TCPFlags = TCPFlags(bytes.slice(12, 14).getInt16 & 0x01ff)
  def windowSize: Short = bytes.slice(14, 16).getInt16 // UNSIGNED
  def checksum: Short = bytes.slice(16, 18).getInt16 // UNSIGNED
  def urgentPointer: Short = bytes.slice(18, 20).getInt16 // UNSIGNED
  def options: IndexedSeq[Byte] = bytes.slice(20, dataOffset * 4)
  def data: IndexedSeq[Byte] = bytes.drop(dataOffset * 4)
}
