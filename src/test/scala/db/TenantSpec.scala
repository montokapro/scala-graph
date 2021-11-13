package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._

class TenantSpec extends AnyFunSpec {
  it("should insert unique record") {
    val program = for {
      _ <- db.Tenant.setup
      _ <- db.Tenant.insert("foo")
      _ <- db.Tenant.insert("foo")
      count <- sql"select count(*) from tenant".query[Int].unique
    } yield count

    val count = transactor.use(program.transact).unsafeRunSync()

    assert(count == 2)
  }
}
