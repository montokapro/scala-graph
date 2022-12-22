package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection
import fs2._
import scala.collection.immutable.ArraySeq

object Junction {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE junction (
        key BYTEA NOT NULL,
        value BYTEA NOT NULL,
        PRIMARY KEY (key, value),
        UNIQUE (value, key)
      )
    """.stripMargin.update.run

  def insert(values: Vector[ByteSeq]): ConnectionIO[ByteSeq] = {
    val key = values.sorted.combineAll.digest
    val rows = values.map((key, _))
    val sql = "INSERT INTO junction (key, value) VALUES (?, ?) ON CONFLICT DO NOTHING"
    Update[(ByteSeq, ByteSeq)](sql).updateMany(rows) *> connection.pure(key)
  }

  def lookupValues(key: ByteSeq): Stream[ConnectionIO, ByteSeq] = {
    sql"select value from junction where key = $key".query[ByteSeq].stream
  }

  def lookupKeys(value: ByteSeq): Stream[ConnectionIO, ByteSeq] = {
    sql"select key from junction where value = $value".query[ByteSeq].stream
  }
}
