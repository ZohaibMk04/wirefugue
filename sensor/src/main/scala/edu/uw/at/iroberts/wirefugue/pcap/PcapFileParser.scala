package edu.uw.at.iroberts.wirefugue.pcap

import java.nio.file.Path
import java.time.Instant
import java.util.NoSuchElementException

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{FileIO, Flow, Keep, Source}
import akka.stream.stage._

import scala.concurrent.Future

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/6/17.
  *
  * A Flow-shaped GraphStage which remembers the most-recently
  * received PcapHeader and uses it to emit one UPacket element
  * for each subsequent Packet received.
  *
  * If no PcapHeader has
  * been seen, we silently eat Packets until we get one.
  * The PcapHeader itself is not passed to the outlet.
  *
  * The PcapHeader contains information which we need to
  * interpret each PacketHeader and Packet, particularly
  * the time zone (`thisZone`) and the link-level header
  * type (`network`). The former is done away with by
  * converting to UTC and the latter is sent along with
  * each emitted element.
  *
  * TODO: Come up with a better name than "PacketContextualizer"
  */

object PcapFileParser {
  def apply(): GraphStage[FlowShape[PcapFileRaw.Message, Packet]] =
    new PcapFileParser()
  def after[I](g: GraphStage[FlowShape[I, PcapFileRaw.Message]]): Flow[I, Packet, NotUsed] =
    Flow.fromGraph(g).viaMat(new PcapFileParser())(Keep.none)
  def before[O](g: GraphStage[FlowShape[Packet, O]]): Flow[PcapFileRaw.Message, O, NotUsed] =
    Flow.fromGraph(new PcapFileParser).via(g)
  def fromPath(path: Path): Source[Packet, Future[IOResult]] =
    FileIO.fromPath(path).
      viaMat(PcapFileParserRawFlow())(Keep.left).
      viaMat(PcapFileParser())(Keep.left)
}

class PcapFileParser private () extends GraphStage[FlowShape[PcapFileRaw.Message, Packet]] {
  val in = Inlet[PcapFileRaw.Message]("packetCtxrIn")
  val out = Outlet[Packet]("packetCtxrOut")

  val shape = FlowShape.of(in, out)

  override def createLogic(attributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      import PcapFileRaw._

      sealed trait State
      case object NoContext extends State
      case class Context(pch: PcapHeader) extends State

      var state: State = NoContext

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val m = grab(in)
          m match {
            case pch: PcapHeader =>
              state = Context(pch)
              pull(in)
            case p: RawPacket =>
              state match {
                case Context(pch: PcapHeader) =>
                  val network = try {
                    LinkType(pch.network.toInt)
                  }
                  catch {
                    case e: NoSuchElementException => LinkType.Other(pch.network.toInt)
                  }
                  val epochSeconds = p.header.tsEpochSeconds + p.pcapHeader.thisZone * 60
                  val nanos = p.header.tsMicroseconds * 1000
                  push(out, Packet(
                    Instant.ofEpochSecond(epochSeconds, nanos.toInt),
                    network,
                    p.header.originalLength,
                    p.packetBytes
                  ))
                case NoContext => pull(in)
              }
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })

    }

}

object PcapFileParserDemo extends App {
  import java.nio.file.Paths

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val ioF = FileIO.fromPath(Paths.get("sensor/src/main/resources/bigFlows.pcap"))
    .via(PcapFileParserRawFlow())
    .via(PcapFileParser())
    .alsoTo(Sink.foreach(println))
    .runWith(Sink.onComplete { _ => system.terminate() })

}
