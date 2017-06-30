package edu.uw.at.iroberts.wirefugue.analyze.count

import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink}
import edu.uw.at.iroberts.wirefugue.Timestamped
import edu.uw.at.iroberts.wirefugue.analyze.metrics.TimestampedWindowAggregator
import edu.uw.at.iroberts.wirefugue.analyze.metrics.TimestampedWindowAggregator.{Aggregator, Window, WindowAttributes}
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

  import Packet.PacketsAreTimestamped

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("packet-count-system")
    implicit val materializer = ActorMaterializer()

    val windowAttributes = TimestampedWindowAggregator.WindowAttributes(
      windowSize = 1 seconds,
      windowStep = 1 second,
      maxDelay = 5 seconds
    )

    case class AggregatePacketData(w: Window, numPackets: Long, numBytes: Long, averagePacketSize: Double) {
      def tsToString(ts: Long) = Instant.ofEpochMilli(ts).toString
      override def toString =
        s"${tsToString(w._1)} => ${tsToString(w._2)}: $numPackets packets, $numBytes bytes, avg: $averagePacketSize"
    }

    case class Accumulator(window: Window, nPackets: Long, nBytes: Long) {
      def result = AggregatePacketData(window, nPackets, nBytes, nBytes.toDouble / nPackets )
    }

    val aggregator = new Aggregator[Packet, AggregatePacketData] {
      type Builder = Accumulator

      def empty = Accumulator((0L, 0L), 0, 0)
      def append = (b) => (p: Packet) => Accumulator(b.window, b.nPackets + 1, b.nBytes + p.data.length)
      def result = (b) => b.result
      def withWindow(window: Window) = (b) => b.copy(window = window)
    }

    val uri = getClass.getResource("/bigFlows.pcap").toURI
    val f = PcapSource(uri)
        .via(TimestampedWindowAggregator(windowAttributes, aggregator))
        .alsoTo(Sink.foreach { agg =>
          println(agg.toString)
        })
      .runFold((0L, 0L)) { case ((np, nb), agg) => (np + agg.numPackets, nb + agg.numBytes) }

    try {
      val total = Await.result(f, 60.minutes)
      println(s"$total events total.") //
    }
    finally system.terminate()
  }


}
