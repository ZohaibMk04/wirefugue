package edu.uw.at.iroberts.wirefugue.pcap

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/3/17.
  *
  * A Flow-shaped GraphStage that transforms a stream of ByteStrings
  * representing a libpcap dump file into a stream of PcapMessage
  * elements: PcapHeader is emitted first with the global file
  * info (pcap_hdr_s), then zero or more Packet objects representing
  * pcaprec_hdr_s and the following payload bytes.
  */
object PcapFileParserRaw {
  private lazy val instance = new PcapFileParserRaw()
  def apply() = instance
}
class PcapFileParserRaw private() extends GraphStage[FlowShape[ByteString, PcapMessage]] {
  val in: Inlet[ByteString] = Inlet("bytesIn")
  val out: Outlet[PcapMessage] = Outlet("packetsOut")

  override val shape: FlowShape[ByteString, PcapMessage] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      import PcapFileRaw._

      // A buffer containing all unhandled bytes from upstream
      var buffer: ByteString = ByteString()
      // (^ Is ByteString a good choice here? In the source,
      // akka.util.ByteString claims to "reduce copying of arrays
      // when concatenating", but leaves us with the unsatisfying
      // "TODO: Add performance characteristics"!)

      val maxSnapLen = 65536L

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

      // The main handler. Called on each outlet pull and inlet push.
      def handleData(): Unit = {

        if (buffer.length < state.bytesNeeded) {
          // We have too few bytes to do anything useful, so pull.
          // println(s"not enough bytes available (want ${state.bytesNeeded}, only have ${buffer.length}). calling pull(in).")
          pull(in)
        }
        else {
          // Downstream needs a new element, and we have enough data to proceed.
          state match {
            case ExpectPcapHeader =>
              val (bytes, rest) = buffer.splitAt(pcapHeaderSizeBytes)
              tryParsePcapHeader(bytes) match {
                case Some(pch) =>
                  push(out, pch)
                  buffer = rest
                  state = ExpectPacketHeader(pch)
                case None =>
                  throw new RuntimeException("Failed to parse pcap file header")
              }
            case ExpectPacketHeader(pch) =>
              val (bytes, rest) = buffer.splitAt(packetHeaderSizeBytes)
              implicit val byteOrder = pch.magicNumber.byteOrder
              tryParsePacketHeader(bytes) foreach { ph: PacketHeader =>
                buffer = rest
                // Call ourselves to handle the packet data now
                // that we know the length. Recursion is ok because limited
                // to depth 1.
                state = ExpectPacketData(pch, ph)
                handleData()
              }
            case ExpectPacketData(pch, ph) =>
              val dataSizeBytes = ph.includedLength
              val (data, rest) = buffer.splitAt(dataSizeBytes)
              push(out, RawPacket(pch, ph, data))
              buffer = rest
              state = ExpectPacketHeader(pch)
          }
        }
      }

      // When we are pushed new data, append it to the buffer
      // and call the main handler.
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          // println("onPush() invoked")
          if (isAvailable(in)) {
            val s = grab(in)
            buffer ++= s
            handleData()
          }
        }

      })

      // When we are pulled upon, call the main handler.
      setHandler(out, new OutHandler {
        override def onPull(): Unit = if (isAvailable(out)) {
          //println("onPull() invoked")
          handleData()
        }
      })


    }
}
