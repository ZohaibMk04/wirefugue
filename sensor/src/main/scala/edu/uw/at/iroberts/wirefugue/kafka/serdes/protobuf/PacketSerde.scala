package edu.uw.at.iroberts.wirefugue.kafka.serdes.protobuf

import edu.uw.at.iroberts.wirefugue.protobufs.packet.{Packet, IPv4PacketKey}
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/8/17.
  */
class PacketSerde extends ProtobufSerde[Packet]

class IPv4PacketKeySerde extends ProtobufSerde[IPv4PacketKey]
