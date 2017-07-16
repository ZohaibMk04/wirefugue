package edu.uw.at.iroberts.wirefugue.kafka.serdes

import java.time.Instant
import java.util

import akka.util.ByteString
import edu.uw.at.iroberts.wirefugue.pcap.Packet
import edu.uw.at.iroberts.wirefugue.pcap.PcapFileRaw.LinkType
import org.apache.kafka.common.serialization._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by scala on 7/1/17.
  */
class PacketSerde extends Serde[Packet] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()
  override def close(): Unit = ()

  override def serializer(): Serializer[Packet] = new Serializer[Packet] {
    val longSerializer = new LongSerializer()
    val integerSerializer = new IntegerSerializer()

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()
    override def serialize(topic: String, p: Packet): Array[Byte] = {
      val buf = ArrayBuffer[Byte]()
      buf ++= longSerializer.serialize(topic, p.timestamp.toEpochMilli)
      buf ++= integerSerializer.serialize(topic, p.originalLength)
      buf ++= integerSerializer.serialize(topic, p.network.id)
      buf ++= p.data

      buf.toArray
    }

    override def close(): Unit = ()
  }

  override def deserializer(): Deserializer[Packet] = new Deserializer[Packet] {
    val longDeserializer = new LongDeserializer()
    val integerDeserializer = new IntegerDeserializer()

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

    override def close(): Unit = ()

    override def deserialize(topic: String, data: Array[Byte]): Packet = Packet(
      timestamp = Instant.ofEpochMilli(longDeserializer.deserialize(topic, data.slice(0, 8))),
      originalLength = integerDeserializer.deserialize(topic, data.slice(8, 12)),
      network = LinkType(integerDeserializer.deserialize(topic, data.slice(12, 16))),
      data = ByteString(data.drop(16))
    )

  }

}
