package edu.uw.at.iroberts.wirefugue.protocol.overlay

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 5/6/17.
  */
trait Overlay

object OverlayDemo extends App {
  import javax.xml.bind.DatatypeConverter

  import akka.util.ByteString
  // val packet0: ByteString = ByteString.fromInts(Array.fill(28)(scala.util.Random.nextInt() % 256): _*)
  val packet: ByteString = ByteString.fromArray(
    DatatypeConverter.parseHexBinary(
      "010203040506" +
        "111213141516" +
        "0004" +
        "2021222324252627282930313233343536373839"
    )
  )

  val ethernet = Ethernet(packet)
  println(ethernet.src)
  println(ethernet.dst)
  println(ethernet.etherType)
  println(ethernet.payload)
  ethernet.etherType match {
    case 0x0004 =>
      println(s"length: ${ethernet.payload.length}")
      val ip4 = IPV4Datagram(ethernet.payload)
      println(ip4.src)
      println(ip4.dest)
    case x =>
      println(f"Expected 0x0004, got 0x$x%04x")
  }

}

