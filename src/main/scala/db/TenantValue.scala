package db;

import doobie._
import doobie.implicits._

object TenantValue {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE IF NOT EXISTS tenant_value (
        tenant_id INT NOT NULL,
        value_id VARCHAR NOT NULL,
        PRIMARY KEY(tenant_id, value_id),
        FOREIGN KEY(tenant_id) REFERENCES tenant(id),
        FOREIGN KEY(value_id) REFERENCES value(id)
      )
    """.stripMargin.update.run

  def insert(tenantId: Int, valueId: String): ConnectionIO[Int] =
    sql"insert into tenant_value (tenant_id, value_id) values ($tenantId, $valueId)".update.run

  def insertValue(tenantId: Int, value: String): ConnectionIO[String] =
    for {
      valueId <- Value.insert(value)
      _ <- insert(tenantId, valueId)
    } yield valueId
}