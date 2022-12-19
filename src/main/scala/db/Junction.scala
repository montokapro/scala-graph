package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection
import fs2._

object Junction {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE junction (
        key VARCHAR NOT NULL,
        value VARCHAR NOT NULL,
        PRIMARY KEY(key, value),
        UNIQUE (value, key)
      )
    """.stripMargin.update.run

  def insert(values: Vector[String]): ConnectionIO[String] = {
    val key = digest(values.sorted.combineAll)
    val rows = values.map((key, _))
    val sql = "INSERT INTO junction (key, value) VALUES (?, ?) ON CONFLICT DO NOTHING"
    Update[(String, String)](sql).updateMany(rows) *> connection.pure(key)
  }

  def lookup(key: String): Stream[ConnectionIO, String] = {
    sql"select value from junction where key = $key".query[String].stream
  }
}
