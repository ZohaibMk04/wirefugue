package edu.uw.at.iroberts.pcap

import java.time.Instant
import akka.util.ByteString
import PcapFileRaw.LinkType
/**
  * Created by Ian Robertson <iroberts@uw.edu> on 4/9/17.
  */

/** Packet represents
  * all we care to know about a single pcap file record,
  * regardless of which file it came from.
  * The timestamp is always in UTC with nanosecond
  * precision and the link-layer header type from the pcap
  * file header is included. includedLength is, well, not included
  * because that information can be obtained (in constant time)
  * from data.length.
  */
case class Packet(
                    timestamp: Instant,
                    network: LinkType.Value,
                    originalLength: Int,
                    data: ByteString
                 )
