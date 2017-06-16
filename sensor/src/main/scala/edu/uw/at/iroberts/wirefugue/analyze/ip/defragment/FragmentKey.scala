package edu.uw.at.iroberts.wirefugue.analyze.ip.defragment

import edu.uw.at.iroberts.wirefugue.pcap.IPAddress

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/14/17.
  */
case class FragmentKey(
                        src: IPAddress,
                        dst: IPAddress,
                        id: Short
                      )
