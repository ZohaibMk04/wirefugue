package edu.uw.at.iroberts.wirefugue.protocol.overlay

import java.nio.ByteOrder

import edu.uw.at.iroberts.wirefugue.pcap.ByteSeqOps._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/22/17.
  */
trait UDPHeader {
  def sport: Short
  def dport: Short
  def length: Int
  def checksum: Short
}

case class UDPDatagram(bytes: IndexedSeq[Byte]) extends UDPHeader {
  implicit val byteOrder = ByteOrder.BIG_ENDIAN
  def sport: Short = bytes.slice(0, 2).getInt16
  def dport: Short = bytes.slice(2, 4).getInt16
  def length: Int = bytes.slice(4, 6).getUInt16
  def checksum: Short = bytes.slice(6, 8).getInt16
}
