package edu.uw.at.iroberts

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import akka.testkit.{ImplicitSender, TestKit}
import edu.uw.at.iroberts.pcap._
import PcapFileRaw._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PcapFileParserSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("PcapFileParserSpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val materializer = ActorMaterializer()
  val filePath = Paths.get("src/main/resources/http.cap")

  "A graph" should {
    "be able to sum the number of packets in http.cap" in {
      val sumF = FileIO.fromPath(filePath)
        .via(PcapFileParserRaw())
        .collect { case p: RawPacket => 1 }
        .runWith(Sink.fold(0)(_ + _))

      val sum = Await.result(sumF, 10 seconds)

      sum shouldBe 43
    }
    "be able to sum the number of payload bytes in http.cap" in {

      val sumF = FileIO.fromPath(filePath)
        .via(PcapFileParserRaw())
        .collect { case p: RawPacket => p.packetBytes.length }
        .runWith(Sink.fold(0: Long)(_ + _))

      val sum = Await.result(sumF, 10 seconds)

      sum shouldBe 25091
    }
    "be able to produce a set of all IP endpoints from the packets in http.cap" in {
      val ipsF: Future[Set[IPAddress]] =
        FileIO.fromPath(filePath)
          .via(PcapFileParserRaw())
          .via(PcapFileParser())
          .collect {
            case pkt: Packet if pkt.network == LinkType.ETHERNET =>
              EthernetFrame.parse(pkt.data)
          }
          .collect(Datagram.fromFrame)
        .mapConcat((dg: Datagram) => Set(dg.sourceIP, dg.destinationIP))
        .runFold(Set[IPAddress]())(_ + _)

      val ips: Set[IPAddress] = Await.result(ipsF, 10 seconds)

      ips shouldEqual Set(
        IPAddress("216.239.59.99"),
        IPAddress("145.254.160.237"),
        IPAddress("65.208.228.223"),
        IPAddress("145.253.2.203")
      )
    }
    "be able to sum the number of payload bytes in nlmon-big.pcap," +
      " a big-endian capture file with linux netlink headers" in {
      val sumF: Future[Long] =
        PcapFileParser.fromPath(Paths.get("src/main/resources/nlmon-big.pcap"))
          .map { _.originalLength }
          .runWith(Sink.fold(0: Long)(_ + _))

      val sum = Await.result(sumF, 10 seconds)

      sum shouldBe 10356
    }
  }

}
