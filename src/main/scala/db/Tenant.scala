package db;

import doobie._
import doobie.implicits._
import doobie.free.connection.ConnectionIO

object Tenant {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE tenant (
        id INT AUTO_INCREMENT NOT NULL,
        name VARCHAR NOT NULL,
        PRIMARY KEY(id)
      )
    """.stripMargin.update.run

  def insert(name: String): ConnectionIO[Int] =
    sql"insert into tenant (name) values ($name)".update.withUniqueGeneratedKeys("id")
}