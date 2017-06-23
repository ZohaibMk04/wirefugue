package edu.uw.at.iroberts.wirefugue.analyze.count

import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink}
import edu.uw.at.iroberts.wirefugue.pcap.{Packet, PcapSource, Timestamp}

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

  // Window specified as [from, until) in epoch milliseconds
  type Window = (Long, Long)


  // (Aggregates are monoids with a window)
  trait Aggregator[Event, Result] {
    type Builder

    def empty: Builder
    def append: Builder => Event => Builder
    def result: Builder => Result
    def withWindow(w: Window): Builder => Builder
  }

  object PacketAggregator extends Aggregator[Packet with Timestamp, AggregatePacketData] {
    type Builder = AggregatePacketData

    def empty = AggregatePacketData((0L, 0L), 0, 0)
    def append = (b) => (p: Packet with Timestamp) => AggregatePacketData(b.w, b.numPackets + 1, b.numBytes + p.data.length)
    def result = identity
    def withWindow(window: Window) = (b) => b.copy(w = window)
  }

  trait Timestamped[E] {
    def toMillis(e: E): Long
  }

  implicit object PacketsAreTimestamped extends Timestamped[Packet] {
    def toMillis(p: Packet): Long = p.timestamp.toEpochMilli
  }

  def PacketMetricsGenerator(attributes: WindowAttributes): Flow[Packet with Timestamp, AggregatePacketData, NotUsed] =
    MetricsAggregator[Packet with Timestamp, AggregatePacketData](attributes)(PacketAggregator)


  def MetricsAggregator[E : Timestamped, R]
  (attributes: WindowAttributes)
  (aggregator: Aggregator[E, R]): Flow[E, R, NotUsed] = {

    sealed trait WindowCommand {
      def w: Window
    }

    case class OpenWindow(w: Window) extends WindowCommand
    case class CloseWindow(w: Window) extends WindowCommand
    case class AddToWindow(ev: E, w: Window) extends WindowCommand

    class CommandGenerator(attributes: WindowAttributes) {
      private var watermark = 0L
      private val openWindows = mutable.Set[Window]()

      def forEvent(ev: E): List[WindowCommand] = {
        val ts: Long = implicitly[Timestamped[E]].toMillis(ev)
        watermark = math.max(watermark, ts - attributes.maxDelay.toMillis)
        if (ts < watermark) {
          println(s"Dropping event with timestamp: ${tsToString(ts)}")
          Nil
        } else {
          val eventWindows = attributes.windowsFor(ts)

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

          val addCommands = eventWindows.map(w => AddToWindow(ev, w))

          openCommands.toList ++ closeCommands.toList ++ addCommands.toList
        }
      }
    }


    Flow[E].statefulMapConcat { () =>
      val generator = new CommandGenerator(attributes)
      p => generator.forEvent(p)
    }.groupBy(100, command => command.w)
      .takeWhile(!_.isInstanceOf[CloseWindow])
      .fold(aggregator.empty) {
        case (b, OpenWindow(window)) => aggregator.withWindow(window)(b)
        // always filtered out by takeWhile
        case (b, CloseWindow(_)) => b
        case (b, AddToWindow(ev, _)) => aggregator.append(b)(ev)
      }
        .map(aggregator.result)
      .async
      .mergeSubstreams
  }


  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("packet-count-system")
    implicit val materializer = ActorMaterializer()

    val windowAttributes = WindowAttributes(
      windowSize = 1 seconds,
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

  case class AggregatePacketData(w: Window, numPackets: Long, numBytes: Long) {
    override def toString =
      s"Between ${tsToString(w._1)} and ${tsToString(w._2)}, there were $numPackets packets and $numBytes bytes."
  }

  def tsToString(ts: Long) = Instant.ofEpochMilli(ts).toString

}
