package edu.uw.at.iroberts.wirefugue.pcap

import java.nio.ByteOrder

import akka.util.ByteString

import scala.util.{Failure, Success, Try}
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  *
  * Capture file format described at
  * https://wiki.wireshark.org/Development/LibpcapFileFormat
  */
object PcapFileRaw {
  import ByteSeqOps._

  private[pcap] val pcapHeaderSizeBytes = 24
  private[pcap] val packetHeaderSizeBytes = 16

  object Magic {
    def tryParse(bytes: ByteString): Try[Magic] = {
      import ByteSeqOps._
      implicit val byteOrder = ByteOrder.LITTLE_ENDIAN
      require(bytes.length == 4)
      bytes.getInt32 match {
        case 0xa1b2c3d4 => Success(NormalMagic)
        case 0xd4c3b2a1 => Success(SwabMagic)
        case x => Failure(new RuntimeException(f"bad magic: 0x$x%08x"))
      }
    }

  }
  trait Magic { def byteOrder: ByteOrder }
  case object NormalMagic extends Magic { val byteOrder = ByteOrder.LITTLE_ENDIAN }
  case object SwabMagic extends Magic { val byteOrder = ByteOrder.BIG_ENDIAN }

  /** LinkType, parsed from the UInt32 pcaprec_hdr_s.network
    * field, tells us what sort of data follows pcaprec_hdr_s.
    * Values for this field are described at
    *   http://www.tcpdump.org/linktypes.html
    */
  object LinkType extends Enumeration {
    val ETHERNET = Value(1)     // Ethernet 802.3 headers
    val LINUX_SLL = Value(117)  // Linux "cooked" capture
    val RAW = Value(101)        // Raw IP packet
    // TODO: Add more header types

    // Allow building ad-hoc Values
    def Other(i: Int): Value = Value(i)
  }

  trait PcapMessage

  case class PcapHeader(
                         magicNumber: Magic,  // guint32
                         versionMajor: Int,   // guint16
                         versionMinor: Int,   // guint16
                         thisZone: Int,       // gint32
                         sigFigs: Long,       // guint32
                         snapLen: Int,        // guint32
                         network: Long        // guint32
                       ) extends PcapMessage

  case class PacketHeader(
                           tsEpochSeconds: Long, // guint32
                           tsMicroseconds: Long, // guint32
                           includedLength: Int, // guint32
                           originalLength: Int // guint32
                         ) extends PcapMessage

  case class PacketData(bytes: ByteString) extends PcapMessage

  type Message = PcapMessage

  case class RawPacket(pcapHeader: PcapHeader, header: PacketHeader, packetBytes: ByteString) extends PcapMessage {
    override def toString = s"Packet($header, [${packetBytes.length} bytes]]"
  }

  def parseMagic(bytes: ByteString): Magic =
    Magic.tryParse(bytes).get

  //def swab32(bytes: ByteString): ByteString = bytes.swab32
  def getUInt32BE(bytes: ByteString): Long = bytes.getUInt32BE
  def getUInt32LE(bytes: ByteString): Long = bytes.getUInt32LE
  def getInt32LE(bytes: ByteString): Int = bytes.getInt32LE

  def tryParsePcapHeader(bytes: ByteString): Option[PcapHeader] = {
    require(bytes.length == pcapHeaderSizeBytes)
    if (bytes.length < pcapHeaderSizeBytes) None
    else {
      val magic = Magic.tryParse(bytes.take(4)).get // Throws RE
      implicit val byteOrder: ByteOrder = magic.byteOrder

      val versionMajor: Int = bytes.drop(4).getUInt16
      val versionMinor: Int = bytes.drop(6).getUInt16
      val thisZone: Int = bytes.drop(8).getInt32
      val sigFigs = bytes.drop(12).getUInt32
      val snapLenL: Long = bytes.drop(16).getUInt32
      val snapLen = Math.min(snapLenL, Int.MaxValue.toLong).toInt
      val network = bytes.drop(20).getUInt32

      val pcapHeader = PcapHeader(
        magic,
        versionMajor,
        versionMinor,
        thisZone,
        sigFigs,
        snapLen,
        network
      )
      Some(pcapHeader)
    }
  }

  def tryParsePacketHeader(bytes: IndexedSeq[Byte])
                          (implicit byteOrder: ByteOrder): Option[PacketHeader] = {
    import ByteSeqOps._

    Some(
      PacketHeader(
        tsEpochSeconds = bytes.getUInt32,
        tsMicroseconds = bytes.drop(4).getUInt32,
        includedLength = {
          val x = bytes.drop(8).getUInt32
          if (x.isValidInt) x.toInt
          else throw new RuntimeException(s"Unexpectedly large incl_len in pcaprec_hdr_s: $x")
        },
        originalLength = {
          val x = bytes.drop(12).getUInt32
          if (x.isValidInt) x.toInt
          else throw new RuntimeException(s"Unexpectedly large orig_len in pcaprec_hdr_s: $x")
        }
      )
    )
  }

}
