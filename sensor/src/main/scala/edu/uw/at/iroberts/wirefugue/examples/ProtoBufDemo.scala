package edu.uw.at.iroberts.wirefugue.examples

import com.example.tutorial.test.Person.{PhoneNumber, PhoneType}
import com.example.tutorial.test._
/**
  * Created by scala on 7/1/17.
  */
object ProtoBufDemo extends App {
  val p = new Person(
    "Bob Jones",
    1234,
    Some("bob_jones@example.com"),
    Seq(new PhoneNumber("101-555-1234", Some(PhoneType.HOME)))
  )

  import edu.uw.at.iroberts.wirefugue.pcap.ByteSeqOps._

  val bytes: IndexedSeq[Byte] = p.toByteArray.toIndexedSeq
  println(bytes.mkHexBlock())

  val p2: Person = Person.parseFrom(bytes.toArray)

  println(p2)

}
