package edu.uw.at.iroberts.wirefugue.analyze.metrics

import akka.NotUsed
import akka.stream.scaladsl.Flow
import edu.uw.at.iroberts.wirefugue.Timestamped

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/23/17.
  */
object TimestampedWindowAggregator {
  case class WindowAttributes(
                               windowSize: Duration,
                               windowStep: Duration,
                               maxDelay: Duration
                             ) {
    val WindowsPerEvent = (windowSize.toMillis / windowStep.toMillis).toInt

    def windowsFor(ts: Long): Set[Window] = {
      val firstWindowStart = ts - ts % windowStep.toMillis - windowSize.toMillis + windowStep.toMillis
      (for (i <- 0 until WindowsPerEvent) yield
        (firstWindowStart + i * windowStep.toMillis,
          firstWindowStart + i * windowStep.toMillis + windowSize.toMillis)
        ).toSet
    }
  }

  // Window specified as [from, until) in epoch milliseconds
  type Window = (Long, Long)


  // (Aggregates are monoids with a window)
  trait Aggregator[Event, Result] {
    type Builder

    def empty: Builder
    def append: Builder => Event => Builder
    def result: Builder => Result
    def withWindow(w: Window): Builder => Builder
  }

  def apply[E : Timestamped, R](attributes: WindowAttributes, aggregator: Aggregator[E, R]): Flow[E, R, NotUsed] = {

    sealed trait WindowCommand {
      def w: Window
    }

    case class OpenWindow(w: Window) extends WindowCommand
    case class CloseWindow(w: Window) extends WindowCommand
    case class AddToWindow(ev: E, w: Window) extends WindowCommand

    class CommandGenerator(attributes: WindowAttributes) {
      private var watermark = 0L
      private val openWindows = mutable.Set[Window]()

      def forEvent(ev: E): List[WindowCommand] = {
        val ts: Long = implicitly[Timestamped[E]].toMillis(ev)
        watermark = math.max(watermark, ts - attributes.maxDelay.toMillis)
        if (ts < watermark) {
          println(s"Dropping event with timestamp: ${implicitly[Timestamped[E]].toString()}")
          Nil
        } else {
          val eventWindows = attributes.windowsFor(ts)

          val closeCommands = openWindows.flatMap { ow =>
            if (!eventWindows.contains(ow) && ow._2 < watermark) {
              openWindows.remove(ow)
              Some(CloseWindow(ow))
            } else None
          }

          val openCommands = eventWindows.flatMap { w =>
            if (!openWindows.contains(w)) {
              openWindows.add(w)
              Some(OpenWindow(w))
            } else None
          }

          val addCommands = eventWindows.map(w => AddToWindow(ev, w))

          openCommands.toList ++ closeCommands.toList ++ addCommands.toList
        }
      }
    }


    Flow[E].statefulMapConcat { () =>
      val generator = new CommandGenerator(attributes)
      p => generator.forEvent(p)
    }.groupBy(100, command => command.w)
      .takeWhile(!_.isInstanceOf[CloseWindow])
      .fold(aggregator.empty) {
        case (b, OpenWindow(window)) => aggregator.withWindow(window)(b)
        // always filtered out by takeWhile
        case (b, CloseWindow(_)) => b
        case (b, AddToWindow(ev, _)) => aggregator.append(b)(ev)
      }
      .map(aggregator.result)
      .async
      .mergeSubstreams
  }

}
