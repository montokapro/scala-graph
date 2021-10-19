package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO

/**
 * Value is content-addressed, and immutable. Its records may always be safely cached or read from a replica.
 */
object Value {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE value (
        id VARCHAR NOT NULL,
        value VARCHAR NOT NULL,
        PRIMARY KEY(id)
      )
    """.stripMargin.update.run

  def insert(value: String): ConnectionIO[String] = {
    val id = digest(value)
    sql"insert into value (id, value) values ($id, $value) ON CONFLICT DO NOTHING".update.run *> connection.pure(id)
  }
}