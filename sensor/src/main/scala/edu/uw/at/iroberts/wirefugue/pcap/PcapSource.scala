package edu.uw.at.iroberts.wirefugue.pcap

import java.net.URI
import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}

import scala.concurrent.Future

/**
  * Created by scala on 6/17/17.
  */
object PcapSource {
  def apply(uri: URI): Source[Packet, Future[IOResult]] = {
    val filePath = Paths.get(uri)
    FileIO.fromPath(filePath)
      .via(PcapFileParserRawFlow())
      .via(PcapFileParser())
  }

}
