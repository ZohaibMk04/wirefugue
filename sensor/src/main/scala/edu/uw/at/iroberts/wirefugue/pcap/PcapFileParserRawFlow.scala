package edu.uw.at.iroberts.wirefugue.pcap

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw._

object PcapFileParserRawFlow {
  val maxSnapLen = 65536L

  def createLogic: () => ByteString => List[PcapMessage] = () => {
    // A buffer containing all unhandled bytes from upstream
    var buffer: ByteString = ByteString()

    // The snapLen most recently observed from a PcapHeader, or
    // this sensible default if none seen yet.
    var snapLen = maxSnapLen

    sealed trait State {
      // Each state shall specify its needs in bytes:
      def bytesNeeded: Int
    }
    // The next 24 bytes in the buffer should be the global
    // header of a pcap dump file.
    case object ExpectPcapHeader extends State {
      val bytesNeeded = pcapHeaderSizeBytes
    }
    // The next 16 bytes should encode a PacketHeader. We hold on to
    // this and advance to
    // the next state after learning the number of bytes included
    // in the PacketData, which follow the PacketHeader.
    case class ExpectPacketHeader(fileContext: PcapHeader) extends State {
      val bytesNeeded = packetHeaderSizeBytes
    }
    // The PacketHeader specifies the number of bytes included in the PacketData.
    // If we read all the expected bytes, we yield a Packet.
    case class ExpectPacketData(fileContext: PcapHeader, packetContext: PacketHeader) extends State {
      // Protect ourselves from bad data
      // TODO: seems a little strange to throw while constructing a State... move elsewhere?
      val len = packetContext.includedLength
      if (len > maxSnapLen)
        throw new RuntimeException(s"pcaprec_hdr_s.incl_len is too large! ($len).")
      else if (len < 0)
        throw new RuntimeException(s"pcaprec_hdr_s.incl_len is less than 0! ($len).")
      val bytesNeeded = len
    }

    // Our current state; start by expecting the global file header
    var state: State = ExpectPcapHeader

    // Recursively produce results from the data we have
    def handleData: List[PcapMessage] = {
      // println(s"handleData: state = $state, buffer = $buffer")
      if (buffer.length < state.bytesNeeded) {
        // We have too few bytes to do anything useful, so yield no records
        Nil
      }
      else {
        // Downstream needs a new element, and we have enough data to proceed.
        state match {
          case ExpectPcapHeader =>
            val (bytes, rest) = buffer.splitAt(pcapHeaderSizeBytes)
            tryParsePcapHeader(bytes) match {
              case Some(pch) =>
                buffer = rest
                state = ExpectPacketHeader(pch)
                pch +: handleData
              case None =>
                throw new RuntimeException("Failed to parse pcap file header")
            }
          case ExpectPacketHeader(pch) =>
            val (bytes, rest) = buffer.splitAt(packetHeaderSizeBytes)
            implicit val byteOrder = pch.magicNumber.byteOrder
            tryParsePacketHeader(bytes) match {
              case Some(ph) =>
                buffer = rest
                state = ExpectPacketData(pch, ph)
                // Don't add anything to the result here, just continue
                handleData
              case None => throw new RuntimeException("Failed to parse packet header")
            }
          case ExpectPacketData(pch, ph) =>
            val dataSizeBytes = ph.includedLength
            val (data, rest) = buffer.splitAt(dataSizeBytes)
            buffer = rest
            state = ExpectPacketHeader(pch)
            RawPacket(pch, ph, data) +: handleData
        }
      }
    }

    // Finally, yield a ByteString => List[PcapMessage]
    { bs =>
      // println(s"Got $bs")
      buffer ++= bs
      handleData
    }
  }


  def apply(): Flow[ByteString, PcapMessage, NotUsed] =
    Flow[ByteString].statefulMapConcat[PcapMessage](createLogic)
}

