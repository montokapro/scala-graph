package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection
import fs2._

object Edge {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE edge (
        id BYTEA NOT NULL,
        input BYTEA NOT NULL,
        output BYTEA NOT NULL,
        PRIMARY KEY (id),
        UNIQUE (input, output)
      )
    """.stripMargin.update.run

  def insert(input: Array[Byte], output: Array[Byte]): ConnectionIO[Array[Byte]] = {
    val id = input.concat(output).digest
    sql"insert into edge (id, input, output) values ($id, $input, $output) ON CONFLICT DO NOTHING".update.run *> connection.pure(id)
  }

  def lookup(input: Array[Byte]): Stream[ConnectionIO, Array[Byte]] = {
    sql"select output from edge where input = $input".query[Array[Byte]].stream
  }
}
