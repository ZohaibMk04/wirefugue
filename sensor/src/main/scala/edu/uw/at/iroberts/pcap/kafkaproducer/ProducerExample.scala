package edu.uw.at.iroberts.pcap.kafkaproducer

import java.nio.ByteBuffer
import java.util.Properties

import akka.util.ByteString
import edu.uw.at.iroberts.pcap.IPAddress
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}

import scala.collection.immutable.Set.Set2

/**
  * Created by scala on 5/12/17.
  */
class ProducerExample {
  val props: Properties = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("acks", "all")
  props.put("retries", 0.asInstanceOf[java.lang.Integer])
  props.put("batch.size", 16384.asInstanceOf[java.lang.Integer])
  props.put("linger.ms", 1.asInstanceOf[java.lang.Integer])
  props.put("buffer.memory", 33554432.asInstanceOf[java.lang.Integer])
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  type IPProto = Byte
  type Port = Short
  type ProducerKey = (IPProto, IPAddress, Port, IPAddress, Port)
  type PacketProducerRecord = ProducerRecord[ProducerKey, ByteString]

  val producer: Producer[ProducerKey, Array[Byte]] = new KafkaProducer(props)
  for (i <- 0 until 100) {
    val key: ProducerKey = (4.toByte, IPAddress("192.168.0.1"), 25563.toShort, IPAddress("192.168.0.2"), 80.toShort)
    val someByteString: ByteString = ???
    val value: Array[Byte] = someByteString.toArray
    producer.send(new ProducerRecord[ProducerKey, Array[Byte]]("ipv4-packets", key, value))
  }

  producer.close()
}
