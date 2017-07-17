package edu.uw.at.iroberts.wirefugue.pcap

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.{ImplicitSender, TestKit}
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw._
import edu.uw.at.iroberts.wirefugue.protocol.overlay.{Ethernet, IPV4Datagram}
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
  val uri = getClass.getResource("/http.cap").toURI

  "A graph" should {
    "be able to sum the number of packets in http.cap" in {
      val sumF = PcapSource(uri)
        .runWith(Sink.fold(0)((acc, _) => acc + 1))

      val sum = Await.result(sumF, 10 seconds)

      sum shouldBe 43
    }
    "be able to sum the number of payload bytes in http.cap" in {

      val sumF = PcapSource(uri)
        .runWith(Sink.fold(0: Long)((acc, packet) => acc + packet.data.length))

      val sum = Await.result(sumF, 10 seconds)

      sum shouldBe 25091
    }
    "be able to produce a set of all IP endpoints from the packets in http.cap" in {
      val ipsF: Future[Set[IPAddress]] =
        PcapSource(uri)
          .collect {
            case pkt: Packet if pkt.network == LinkType.ETHERNET =>
              Ethernet(pkt.data)
          }
          .collect {
            case e@Ethernet(_) if e.etherType == EtherType.IPv4.id =>
              IPV4Datagram(e.payload)
          }
              .mapConcat((dg: IPV4Datagram) => Set(dg.src, dg.dest))
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
      val uri = getClass.getResource("/nlmon-big.pcap").toURI
      val sumF: Future[Long] =
        PcapSource(uri)
          .map { _.originalLength }
          .runWith(Sink.fold(0: Long)(_ + _))

      val sum = Await.result(sumF, 10 seconds)

      sum shouldBe 10356
    }
  }

}
