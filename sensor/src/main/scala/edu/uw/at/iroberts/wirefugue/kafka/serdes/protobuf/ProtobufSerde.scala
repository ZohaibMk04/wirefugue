package edu.uw.at.iroberts.wirefugue.kafka.serdes.protobuf

import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import edu.uw.at.iroberts.wirefugue.kafka.serdes.{StatelessDeserializer, StatelessSerde, StatelessSerializer}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/8/17.
  */
class ProtobufSerde[M <: GeneratedMessage with Message[M]](implicit companion: GeneratedMessageCompanion[M])
  extends Serde[M] with StatelessSerde[M] {

  override def serializer(): Serializer[M] = new Serializer[M] with StatelessSerializer[M] {
    override def serialize(topic: String, message: M): Array[Byte] = message.toByteArray
  }

  override def deserializer(): Deserializer[M] = new Deserializer[M] with StatelessDeserializer[M] {
    override def deserialize(topic: String, data: Array[Byte]): M = companion.parseFrom(data)
  }

}
