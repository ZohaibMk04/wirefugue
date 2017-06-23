package edu.uw.at.iroberts.wirefugue.analyze.count

import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.stage.GraphStage
import edu.uw.at.iroberts.wirefugue.pcap.{Packet, PcapSource}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/21/17.
  *
  * Based on https://softwaremill.com/windowing-data-in-akka-streams/
  */
object LivePacketCount {

  case class WindowAttributes(
                             windowSize: Duration,
                             windowStep: Duration,
                             maxDelay: Duration
                             ) {
    val WindowsPerEvent = (windowSize.toMillis / windowStep.toMillis).toInt

    def windowsFor(ts: Long): Set[Window] = {
      val firstWindowStart = ts - ts % windowStep.toMillis - windowSize.toMillis + windowStep.toMillis
      (for (i <- 0 until WindowsPerEvent) yield
        (firstWindowStart + i * windowStep.toMillis,
          firstWindowStart + i * windowStep.toMillis + windowSize.toMillis)
        ).toSet
    }
  }

  def PacketMetricsGenerator(attributes: WindowAttributes): Flow[Packet, AggregatePacketData, NotUsed] =
    Flow[Packet].statefulMapConcat { () =>
      val generator = new CommandGenerator(attributes)
      p => generator.forPacket(p)
    }.groupBy(100, command => command.w)
      .takeWhile(!_.isInstanceOf[CloseWindow])
      .fold(AggregatePacketData((0L, 0L), 0, 0)) {
        case (agg, OpenWindow(window)) => agg.copy(w = window)
        // always filtered out by takeWhile
        case (agg, CloseWindow(_)) => agg
        case (agg, AddToWindow(ev, _)) => agg.copy(
          numPackets = agg.numPackets + 1,
          numBytes = agg.numBytes + ev.data.length
        )
      }
      .async
      .mergeSubstreams


  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("packet-count-system")
    implicit val materializer = ActorMaterializer()

    val windowAttributes = WindowAttributes(
      windowSize = 10 seconds,
      windowStep = 1 second,
      maxDelay = 5 seconds
    )
    val uri = getClass.getResource("/bigFlows.pcap").toURI
    val f = PcapSource(uri)
        .via(PacketMetricsGenerator(windowAttributes))
        .alsoTo(Sink.foreach { agg =>
          println(agg.toString)
        })
      .runFold((0L, 0L)) { case ((np, nb), agg) => (np + agg.numPackets, nb + agg.numBytes) }

    try {
      val total = Await.result(f, 60.minutes)
      println(s"$total events total.")
    }
    finally system.terminate()
  }

  // Window specified as [from, until) in epoch milliseconds
  type Window = (Long, Long)

  sealed trait WindowCommand {
    def w: Window
  }

  case class OpenWindow(w: Window) extends WindowCommand
  case class CloseWindow(w: Window) extends WindowCommand
  case class AddToWindow(p: Packet, w: Window) extends WindowCommand

  class CommandGenerator(attributes: WindowAttributes) {
    private var watermark = 0L
    private val openWindows = mutable.Set[Window]()

    def forPacket(p: Packet): List[WindowCommand] = {
      watermark = math.max(watermark, p.timestamp.toEpochMilli - attributes.maxDelay.toMillis)
      if (p.timestamp.toEpochMilli < watermark) {
        println(s"Dropping event with timestamp: ${tsToString(p.timestamp.toEpochMilli)}")
        Nil
      } else {
        val eventWindows = attributes.windowsFor(p.timestamp.toEpochMilli)

        val closeCommands = openWindows.flatMap { ow =>
          if (!eventWindows.contains(ow) && ow._2 < watermark) {
            openWindows.remove(ow)
            Some(CloseWindow(ow))
          } else None
        }

        val openCommands = eventWindows.flatMap { w =>
          if (!openWindows.contains(w)) {
            openWindows.add(w)
            Some(OpenWindow(w))
          } else None
        }

        val addCommands = eventWindows.map(w => AddToWindow(p, w))

        openCommands.toList ++ closeCommands.toList ++ addCommands.toList
      }
    }
  }

  case class AggregatePacketData(w: Window, numPackets: Long, numBytes: Long) {
    override def toString =
      s"Between ${tsToString(w._1)} and ${tsToString(w._2)}, there were $numPackets packets and $numBytes bytes."
  }

  def tsToString(ts: Long) = Instant.ofEpochMilli(ts).toString

}
