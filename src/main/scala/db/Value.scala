package db;

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection

/** Value is content-addressed, and immutable. Its records may always be safely
  * cached or read from a replica.
  */
object Value {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE value (
        id BYTEA NOT NULL,
        value BYTEA NOT NULL,
        PRIMARY KEY (id)
      )
    """.stripMargin.update.run

  def insert(value: ByteSeq): ConnectionIO[ByteSeq] = {
    val id = value.digest
    sql"insert into value (id, value) values ($id, $value) ON CONFLICT DO NOTHING".update.run *> connection
      .pure(id)
  }
}
