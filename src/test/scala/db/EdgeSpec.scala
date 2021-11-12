package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._
import doobie.h2._

class EdgeSpec extends AnyFunSpec {
  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, Transactor[IO]] = {
    val url = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](1)
      xa <- H2Transactor.newH2Transactor[IO](url, "username", "password", ec)
    } yield Transactor.after.set(xa, HC.rollback)
  }

  it("should insert idempotently") {
    val program = for {
      _ <- db.Edge.setup
      _ <- db.Edge.insert("foo", "bar")
      _ <- db.Edge.insert("foo", "bar")
      edge <- sql"select input, output from edge".query[(String, String)].unique
    } yield edge

    val (input, output) = transactor.use(program.transact).unsafeRunSync()

    assert(input == "foo")
    assert(output == "bar")
  }
}
