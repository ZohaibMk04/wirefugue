package edu.uw.at.iroberts.wirefugue.analyze.ip.defragment

import akka.actor.{Actor, ActorRef}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.util.{ByteString, ByteStringBuilder}
import edu.uw.at.iroberts.wirefugue.pcap.{IPAddress, InternetChecksum}
import edu.uw.at.iroberts.wirefugue.protocol.overlay.IPV4Datagram

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/30/17.
  */
object Defragment extends GraphStage[FlowShape[IPV4Datagram, IPV4Datagram]] {
  val in = Inlet[IPV4Datagram]("fragmented-packets-in")
  val out = Outlet[IPV4Datagram]("non-fragmented-packets-out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(attrs: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var defragger: Map[(IPAddress, IPAddress, Short), Actor] = ???

    // For each packet received:
    //   1. If MF = 0 && offset = 0 pass the packet as-is
    //   2. If MF = 0 set totalLength to offset + length
    //   3. otherwise, store the payload at the offset
    //   4. check for missing fragments, if none, concat & send

  }
}

class Defragger(key: FragmentKey, sender: ActorRef) extends Actor {
  private var dataLength: Option[Int] = None
  private var bytesYet: Int = 0

  private var fragments: SortedMap[Int, IndexedSeq[Byte]] = SortedMap.empty
  private var firstHeader: Option[IndexedSeq[Byte]] = None

  private[this] def missingFragments: Boolean = {
    @tailrec
    def r(next: Int): Boolean = fragments.get(next) match {
      case Some(f) => r(next + f.length + 1)
      case None => true
    }
    if (dataLength.isEmpty || firstHeader.isEmpty) true else r(0)
  }

  private[this] def assembledPayload: IndexedSeq[Byte] = {
    val b: ByteStringBuilder = ByteString.createBuilder
    fragments.foldLeft((b, 0)) {
      case ((b, next),(offset,bytes)) => (b ++= bytes, offset + bytes.length)
    }
    b.result()
  }

  override def receive = {
    case p: IPV4Datagram =>

      val isLastFragment = !p.flagMF
      val isFirstFragment = p.offset == 0

      if (isLastFragment)
        dataLength = Some(p.offset*8 + p.data.length)
      if (isFirstFragment)
        firstHeader = Some(p.bytes.take(p.ihl * 4))

      fragments += p.offset * 8 -> p.data

      if (!missingFragments) {
        // Starting with the header for the first fragment, remove the MF flag,
        // update totalLength, recompute header checksum.
        // TODO This won't be pretty until we have a proper builder for packets
        val headerBytes = IPV4Datagram(firstHeader.get).ihl * 4
        val totalLength = headerBytes + dataLength.get
        val newPacket: Array[Byte] = new Array[Byte](totalLength)
        firstHeader.get.copyToArray(newPacket)
        newPacket(2) = (totalLength >>> 8).toByte
        newPacket(3) = totalLength.toByte
        newPacket(7) = (newPacket(7) & ~0x20).toByte
        newPacket(10) = 0
        newPacket(11) = 0
        // FIXME bad idea to _.take on a java.lang.Array?
        val checksum = InternetChecksum.internetChecksum(newPacket.take(headerBytes))
        newPacket(10) = (checksum >>> 8).toByte
        newPacket(11) = checksum.toByte

        sender ! IPV4Datagram(ByteString(newPacket))
      }
  }
}

