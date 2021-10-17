package db;

import doobie._
import doobie.implicits._
import doobie.free.connection.ConnectionIO

object TenantValue {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE tenant_value (
        tenant_id INT NOT NULL,
        value_id VARCHAR NOT NULL,
        PRIMARY KEY(tenant_id, value_id)
      )
    """.stripMargin.update.run

  def insert(tenantId: Int, valueId: String): ConnectionIO[Int] =
    sql"insert into tenant_value (name) values ($tenantId, $valueId)".update.run
}