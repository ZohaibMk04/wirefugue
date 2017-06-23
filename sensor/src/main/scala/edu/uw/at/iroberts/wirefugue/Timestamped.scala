package edu.uw.at.iroberts.wirefugue

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/23/17.
  */
trait Timestamped[E] {
  def toMillis(e: E): Long
  def toString(e: E) = toMillis(e).toString
}

