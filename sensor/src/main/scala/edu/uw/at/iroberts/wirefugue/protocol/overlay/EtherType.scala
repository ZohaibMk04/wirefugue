package edu.uw.at.iroberts.wirefugue.protocol.overlay

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/17/17.
  */
object EtherType extends Enumeration {
  val IPv4 = Value(0x0800)
  val IPv6 = Value(0x86dd)
  val ARP = Value(0x0806)
  // TODO: Add more EtherType values
  def other(i: Int): EtherType.Value = Value(i)
}
