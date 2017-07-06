package edu.uw.at.iroberts.wirefugue.sink

import akka.stream.scaladsl.{Sink => AkkaSink}

import scala.concurrent.Future

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/29/17.
  */
private[wirefugue] object Sink {
  def count[T]: AkkaSink[T, Future[Long]] =
    AkkaSink.fold[Long, T](0L) {
      (x: Long, y: T) => x + 1
    }
  def count[T, U : Numeric]: AkkaSink[T, Future[U]] =
    AkkaSink.fold[U, T](implicitly[Numeric[U]].fromInt(0)) {
      (x: U, y: T) => implicitly[Numeric[U]].plus(x, implicitly[Numeric[U]].fromInt(1))
    }
}
