package alg;

import cats.effect._
import db._
import doobie._
import doobie.implicits._
import fs2._

class Choice(val xa: Transactor[IO]) {
  def insert(values: Vector[ByteSeq]): IO[ByteSeq] =
    Junction.insert(values).transact(xa)

  def lookupValues(key: ByteSeq): Stream[IO, ByteSeq] =
    Junction.lookupValues(key).transact(xa)

  def lookupKeys(value: ByteSeq): Stream[IO, ByteSeq] =
    Junction.lookupKeys(value).transact(xa)
}
