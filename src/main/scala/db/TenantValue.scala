package db;

import doobie._
import doobie.implicits._

object TenantValue {
  val setup: ConnectionIO[Int] =
    sql"""
      CREATE TABLE tenant_value (
        tenant_id INT NOT NULL,
        value_id BYTEA NOT NULL,
        PRIMARY KEY(tenant_id, value_id),
        FOREIGN KEY(tenant_id) REFERENCES tenant(id),
        FOREIGN KEY(value_id) REFERENCES value(id)
      )
    """.stripMargin.update.run

  def insert(tenantId: Int, valueId: ByteSeq): ConnectionIO[Int] =
    sql"insert into tenant_value (tenant_id, value_id) values ($tenantId, $valueId)".update.run

  def insertValue(tenantId: Int, value: ByteSeq): ConnectionIO[ByteSeq] =
    for {
      valueId <- Value.insert(value)
      _ <- insert(tenantId, valueId)
    } yield valueId
}
