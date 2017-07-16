package edu.uw.at.iroberts.wirefugue.kafka

import java.time.Instant

import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.pcap.Packet
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw.LinkType
import edu.uw.at.iroberts.wirefugue.protobufs.packet.{Packet => ProtobufPacket}

package object producer {
  private val nanosPerSecond = 1000000000L

  def instantToEpochNanos(ts: Instant): Long = ts.getEpochSecond * nanosPerSecond + ts.getNano

  def packetToProtobufPacket(packet: Packet): ProtobufPacket = {
    ProtobufPacket.defaultInstance
      .withSensorTsEpochNanos(instantToEpochNanos(packet.timestamp))
      .withData(com.google.protobuf.ByteString.copyFrom(packet.data.toArray))
      .withOriginalLength(packet.originalLength)
      .withLinkType(packet.network.id)
  }

  def protobufPacketToPacket(pp: ProtobufPacket): Option[Packet] = for {
    nanos <- pp.sensorTsEpochNanos
    data <- pp.data
    originalLength <- pp.originalLength
    linkType <- pp.linkType
  } yield Packet(
      timestamp = Instant.ofEpochSecond(nanos / nanosPerSecond, nanos % nanosPerSecond),
      network = LinkType(linkType),
      originalLength = data.size,
      data = ByteString.fromByteBuffer(data.asReadOnlyByteBuffer())
    )


}