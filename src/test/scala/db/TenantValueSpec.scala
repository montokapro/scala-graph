package db;

import org.scalatest.funspec.AnyFunSpec

import cats.implicits._
import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._

class TenantValueSpec extends AnyFunSpec {
  describe("insertValue") {
    it("should insert record") {
      val program = for {
        _ <- List(db.Tenant.setup, db.Value.setup, db.TenantValue.setup).sequence
        tenantId <- db.Tenant.insert("Alice")
        valueId <- db.Value.insert("foo")
        _ <- db.TenantValue.insert(tenantId, valueId)
        count <- sql"select count(*) from tenant_value".query[Int].unique
      } yield count

      val count = transactor.use(program.transact).unsafeRunSync()

      assert(count == 1)
    }

    it("should error on missing tenant id") {
      val program = for {
        _ <- List(db.Tenant.setup, db.Value.setup, db.TenantValue.setup).sequence
        valueId <- db.Value.insert("foo")
        _ <- db.TenantValue.insert(-1, valueId)
      } yield ()

      assertThrows[java.sql.SQLIntegrityConstraintViolationException] {
        transactor.use(program.transact).unsafeRunSync()
      }
    }

    it("should error on missing value id") {
      val program = for {
        _ <- List(db.Tenant.setup, db.Value.setup, db.TenantValue.setup).sequence
        tenantId <- db.Tenant.insert("Alice")
        _ <- db.TenantValue.insert(tenantId, "")
      } yield ()

      assertThrows[java.sql.SQLIntegrityConstraintViolationException] {
        transactor.use(program.transact).unsafeRunSync()
      }
    }

    it("should error on duplicate insert") {
      val program = for {
        _ <- List(db.Tenant.setup, db.Value.setup, db.TenantValue.setup).sequence
        tenantId <- db.Tenant.insert("Alice")
        valueId <- db.Value.insert("foo")
        _ <- db.TenantValue.insert(tenantId, valueId)
        _ <- db.TenantValue.insert(tenantId, valueId)
      } yield ()

      assertThrows[java.sql.SQLIntegrityConstraintViolationException] {
        transactor.use(program.transact).unsafeRunSync()
      }
    }
  }

  describe("insertValue") {
    it("should insert idempotent values with unique records") {
      val program = for {
        _ <- List(db.Tenant.setup, db.Value.setup, db.TenantValue.setup).sequence
        aliceId <- db.Tenant.insert("Alice")
        bobId <- db.Tenant.insert("Bob")
        _ <- db.TenantValue.insertValue(aliceId, "foo")
        _ <- db.TenantValue.insertValue(bobId, "foo")
        tenantCount <- sql"select count(*) from tenant".query[Int].unique
        tenantValueCount <- sql"select count(*) from tenant_value".query[Int].unique
        valueCount <- sql"select count(*) from value".query[Int].unique
      } yield (tenantCount, tenantValueCount, valueCount)

      val (tenantCount, tenantValueCount, valueCount) = transactor.use(program.transact).unsafeRunSync()

      assert(tenantCount == 2)
      assert(tenantValueCount == 2)
      assert(valueCount == 1)
    }
  }
}
