package edu.uw.at.iroberts.wirefugue.kafka.serdes

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 7/8/17.
  */
trait StatelessSerde[T] { self: Serde[T] =>
  override def configure(configs: util.Map[String, _], isKey: Boolean) = ()
  override def close() = ()
}

trait StatelessSerializer[T] { self: Serializer[T] =>
  override def configure(configs: util.Map[String, _], isKey: Boolean) = ()
  override def close() = ()
}

trait StatelessDeserializer[T] { self: Deserializer[T] =>
  override def configure(configs: util.Map[String, _], isKey: Boolean) = ()
  override def close() = ()
}
