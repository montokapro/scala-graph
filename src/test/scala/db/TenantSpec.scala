package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._
import doobie.h2._

class TenantSpec extends AnyFunSpec {
  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  def transactor: Resource[IO, Transactor[IO]] = {
    val databaseName = scala.util.Random.alphanumeric.take(16).mkString

    val url = s"jdbc:h2:mem:$databaseName;MODE=PostgreSQL"

    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](1)
      xa <- H2Transactor.newH2Transactor[IO](url, "username", "password", ec)
    } yield xa
  }

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
