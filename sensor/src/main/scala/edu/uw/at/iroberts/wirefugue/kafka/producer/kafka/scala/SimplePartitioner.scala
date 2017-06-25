package edu.uw.at.iroberts.wirefugue.kafka.producer.kafka.scala

import org.apache.kafka.clients.producer
import org.apache.kafka.common.Cluster

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/15/17.
  */
trait SimplePartitioner extends producer.Partitioner {
  // Provide default do-nothing methods for configure and close
  def configure(configs: java.util.Map[String, _]) = ()
  def close() = ()

  def partition(topic: String,
            key: Option[Any],
            keyBytes: Option[IndexedSeq[Byte]],
            value: Option[Any],
            valueBytes: Option[IndexedSeq[Byte]],
            cluster: Cluster): Int

  override def partition(
                        topic: String,
                        key: Object,
                        keyBytes: Array[Byte],
                        value: Object,
                        valueBytes: Array[Byte],
                        cluster: Cluster
                        ): Int = partition(
    topic,
    Option(key),
    Option(keyBytes).map(_.toIndexedSeq),
    Option(value),
    Option(valueBytes).map(_.toIndexedSeq),
    cluster
  )
}
