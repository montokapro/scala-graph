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

  def insert(input: ByteSeq, output: ByteSeq): ConnectionIO[ByteSeq] = {
    val id = (input |+| output).digest
    sql"insert into edge (id, input, output) values ($id, $input, $output) ON CONFLICT DO NOTHING".update.run *> connection.pure(id)
  }

  def lookup(input: ByteSeq): Stream[ConnectionIO, ByteSeq] = {
    sql"select output from edge where input = $input".query[ByteSeq].stream
  }
}
