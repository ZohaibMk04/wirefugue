package edu.uw.at.iroberts.wirefugue

import java.time.Instant

/**
  * Created by Ian Robertson <iroberts@uw.edu> on 6/23/17.
  *
  * Type class for things that are temporally located
  */
trait Timestamped[E] {
  def timestamp(e: E): Instant
}

