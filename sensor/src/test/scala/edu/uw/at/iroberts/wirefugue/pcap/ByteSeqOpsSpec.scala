package edu.uw.at.iroberts.wirefugue.pcap

import org.scalatest.{Matchers, WordSpecLike}

/**
  * Created by scala on 5/22/17.
  */
class ByteSeqOpsSpec extends WordSpecLike with Matchers {
  "toBytesBE" should {
    "serialize a 32-bit integer in big-endian format" in {
      import ByteSeqOps.toBytesBE
      val expected = Array(0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xbe.toByte)
      toBytesBE(0xcafebabe.toInt) shouldEqual expected

    }
    "serialize a 16-bit integer in big-endian format" in {
      import ByteSeqOps.toBytesBE
      val expected = Array(0xbe.toByte, 0xef.toByte)
      toBytesBE(0xbeef.toShort) shouldEqual expected
    }
  }

}
