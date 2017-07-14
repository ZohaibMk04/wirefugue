package edu.uw.at.iroberts.wirefugue.pcap

import org.scalatest.{Matchers, WordSpecLike}

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/7/17.
  */
class IPAddressSpec extends WordSpecLike with Matchers {
  val ipString: String = "192.168.42.196"
  val ipBytes: Seq[Byte] = Seq(0xc0, 0xa8, 0x2a, 0xc4).map(_.toByte)
  "an IPAddress" should {
    "be constructable from String and Seq[Byte]" in {
      IPAddress(ipString) shouldEqual IPAddress(ipBytes)
    }

    "produce a string representation" in {
      val a = IPAddress(ipBytes)

      a.toString shouldEqual ipString
    }

    "produce a byte sequence representation" in {
      val a = IPAddress(ipString)

      a.bytes shouldEqual ipBytes
    }

    "provide an equivalent java.net.Inet4Address" in {
      import java.net.{InetAddress, Inet4Address}

      val a = IPAddress(ipString)

      a.inet4Address shouldEqual InetAddress.getByAddress(ipBytes.toArray).asInstanceOf[Inet4Address]
    }

    "reject invalid sequences upon construction" in {
      assertThrows[IllegalArgumentException] {
        IPAddress(Seq[Byte](0x10, 0x20, 0x30, 0x40, 0x50))
      }
    }

    "reject invalid strings upon construction" in {
      assertThrows[IllegalArgumentException] {
        IPAddress("192.168.0.256")
      }
      assertThrows[IllegalArgumentException] {
        IPAddress("10.0.0.0.1")
      }
      assertThrows[IllegalArgumentException] {
        IPAddress("192.168.2")
      }
    }
  }

}
