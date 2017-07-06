package edu.uw.at.iroberts.wirefugue.pcap.example

import java.io.{PrintWriter, StringWriter}
import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{Keep, RunnableGraph}
import edu.uw.at.iroberts.wirefugue.pcap.{Packet, PcapSource}
import edu.uw.at.iroberts.wirefugue.sink.Sink

import scala.concurrent._
import scala.util.{Failure, Success}

/** Count the number of records in a capture file.
  *
  * Created by Ian Robertson <iroberts@uw.edu> on 6/29/17.
  */
object CountRecords extends App {
  private def stackTraceAsString(e: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }

  require(args.length == 1)

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val uri = Paths.get(args(0)).toUri
  val graph: RunnableGraph[(Future[IOResult], Future[Long])] = PcapSource(uri).toMat(Sink.count[Packet, Long])(Keep.both)

  val (ioF, countF) = graph.run()

  implicit val ec: ExecutionContext = system.dispatcher

  ioF.onComplete {
    case Success(result) =>
      println(s"IO operation read ${result.count} bytes and " + (result.status match {
        case Success(Done) => "completed successfully."
        case Failure(ex) => "threw an exception: " + stackTraceAsString(ex)
      }))
    case Failure(ex) =>
      println("Source threw an exception: ")
      println(ex.printStackTrace())
  }

  countF.onComplete {
    case Success(count) => println(s"$count records received")
    case Failure(ex) => ex.printStackTrace()
  }

  try {
    import scala.concurrent.duration._

    Await.ready(countF, 60 minutes)
  }
  finally {
    system.terminate()
  }

}
