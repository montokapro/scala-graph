package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection
import fs2._
import db.ArrayOrdering

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

  def insert(values: Vector[Array[Byte]]): ConnectionIO[Array[Byte]] = {
    val key = values.sorted.combineAll.digest
    val rows = values.map((key, _))
    val sql = "INSERT INTO junction (key, value) VALUES (?, ?) ON CONFLICT DO NOTHING"
    Update[(Array[Byte], Array[Byte])](sql).updateMany(rows) *> connection.pure(key)
  }

  def lookup(key: Array[Byte]): Stream[ConnectionIO, Array[Byte]] = {
    sql"select value from junction where key = $key".query[Array[Byte]].stream
  }
}
