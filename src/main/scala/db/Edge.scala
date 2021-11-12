package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO

object Edge {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE IF NOT EXISTS edge (
        input VARCHAR NOT NULL,
        output VARCHAR NOT NULL,
        PRIMARY KEY(input, output)
      )
    """.stripMargin.update.run

  def insert(input: String, output: String): ConnectionIO[Int] = {
    sql"insert into edge (input, output) values ($input, $output) ON CONFLICT DO NOTHING".update.run
  }
}