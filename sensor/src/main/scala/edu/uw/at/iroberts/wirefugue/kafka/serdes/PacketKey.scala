package edu.uw.at.iroberts.wirefugue.kafka.serdes

import edu.uw.at.iroberts.wirefugue.pcap.IPAddress

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
                    ) extends Serializable