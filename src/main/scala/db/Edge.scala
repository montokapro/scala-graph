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
        id VARCHAR NOT NULL,
        input VARCHAR NOT NULL,
        output VARCHAR NOT NULL,
        PRIMARY KEY (id),
        UNIQUE (input, output)
      )
    """.stripMargin.update.run

  def insert(input: String, output: String): ConnectionIO[String] = {
    val id = digest(input + output)
    sql"insert into edge (id, input, output) values ($id, $input, $output) ON CONFLICT DO NOTHING".update.run *> connection.pure(id)
  }

  def lookup(input: String): Stream[ConnectionIO, String] = {
    sql"select output from edge where input = $input".query[String].stream
  }
}
