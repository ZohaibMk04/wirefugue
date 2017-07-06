package edu.uw.at.iroberts.wirefugue.kafka.serdes

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util

import org.apache.kafka.common.serialization._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/1/17.
  */
class PacketKeySerializer extends Serializer[PacketKey] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()
  override def close(): Unit = ()

  override def serialize(topic: String, data: PacketKey): Array[Byte] = {
    val bs = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bs)
    oos.writeObject(data)

    bs.toByteArray
  }
}

class PacketKeyDeserializer extends Deserializer[PacketKey] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()
  override def close(): Unit = ()

  override def deserialize(topic: String, data: Array[Byte]): PacketKey = {
    val bs = new ByteArrayInputStream(data)
    val ois = new ObjectInputStream(bs)

    ois.readObject().asInstanceOf[PacketKey]
  }
}

class PacketKeySerde extends Serde[PacketKey] {
  val serializer = new PacketKeySerializer
  val deserializer = new PacketKeyDeserializer

  def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    serializer.configure(configs, isKey)
    deserializer.configure(configs, isKey)
  }
  def close(): Unit = {
    serializer.close()
    deserializer.close()
  }
}
