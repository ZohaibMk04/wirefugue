package edu.uw.at.iroberts.wirefugue.kafka.serdes

import edu.uw.at.iroberts.wirefugue.pcap.IPAddress

import scala.util.hashing.{Hashing, MurmurHash3}

/**
  * Created by scala on 7/1/17.
  */
@SerialVersionUID(11223344)
case class PacketKey(
                    protocol: Byte,
                    sourceIP: IPAddress,
                    sourcePort: Short,
                    destinationIP: IPAddress,
                    destinationPort: Short
                    ) extends Serializable {
  override def hashCode(): Int = PacketKey.packetKeyHashing.hash(this)

}

object PacketKey {
  def packetKeyHashing = new Hashing[PacketKey] {
    def hash(pk: PacketKey) = (pk.protocol ->
      Set(
        pk.sourceIP -> pk.sourcePort,
        pk.destinationIP -> pk.destinationPort
      )).##
  }
}

object SimpleTest extends App {
  val p1 = PacketKey(
    3.toByte,
    IPAddress("10.0.0.1"),
    123.toShort,
    IPAddress("192.168.0.1"),
    456.toShort)
  val p2 = PacketKey(
    3.toByte,
    IPAddress("192.168.0.1"),
    456.toShort,
    IPAddress("10.0.0.1"),
    123.toShort,
  )
  println(p1.##)
  println(p2.##)
}