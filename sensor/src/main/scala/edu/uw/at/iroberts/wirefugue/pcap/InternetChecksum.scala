package edu.uw.at.iroberts.wirefugue.pcap

/** Methods to compute and validate RFC 1071 Internet checksums.
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 5/18/17.
  */
object InternetChecksum {
  def onesSum(data: IndexedSeq[Byte]): Short = {
    import ByteSeqOps._

    // Try to be somewhat efficient here. Summing 32-bit words
    // into a 64-bit accumulator would probably be better
    // for 64-bit architectures.
    val length = data.length
    val mod2 = length % 2
    var sum: Int = data
      .slice(0, length - mod2)
      .grouped(2)
      .map(_.getUInt16BE)
      .sum
    if (mod2 != 0)
      sum += data.last << 8

    ((sum & 0xffff) + (sum >>> 16)).toShort
  }

  // Results in the one's complement of onesSum(data) as a 16-bit integer,
  // which is the value that should be placed in the checksum field.
  // If the checksum field is part of the data, it should be set to 0
  // before calculating the checksum.
  def internetChecksum(data: IndexedSeq[Byte]): Short = (~onesSum(data)).toShort

  // When a zeroed field included in the sum is replaced by a valid checksum,
  // the checksum of the data will be 0.
  def checksumValid(data: IndexedSeq[Byte]): Boolean = internetChecksum(data) == 0
}
