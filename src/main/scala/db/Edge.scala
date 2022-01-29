package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import fs2._

object Edge {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE edge (
        input VARCHAR NOT NULL,
        output VARCHAR NOT NULL,
        PRIMARY KEY(input, output)
      )
    """.stripMargin.update.run

  def insert(input: String, output: String): ConnectionIO[Int] = {
    sql"insert into edge (input, output) values ($input, $output) ON CONFLICT DO NOTHING".update.run
  }

  def lookup(input: String): Stream[ConnectionIO, String] = {
    sql"select output from edge where input = $input".query[String].stream
  }
}
