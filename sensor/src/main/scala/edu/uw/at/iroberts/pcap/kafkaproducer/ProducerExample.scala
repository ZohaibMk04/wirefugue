package edu.uw.at.iroberts.pcap.kafkaproducer

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}

/**
  * Created by scala on 5/12/17.
  */
class ProducerExample {
  val props: Properties = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("acks", "all")
  props.put("retries", 0)
  props.put("batch.size", 16384)
  props.put("linger.ms", 1)
  props.put("buffer.memory", 33554432)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer: Producer[String, String] = new KafkaProducer(props)
  for (i <- 0 until 100) {
    producer.send(new ProducerRecord[String, String]("my-topic", Integer.toString(i), Integer.toString(i)))
  }

  producer.close()
}
