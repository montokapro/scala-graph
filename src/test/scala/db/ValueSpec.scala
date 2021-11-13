package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._

class ValueSpec extends AnyFunSpec {
  it("should insert long value") {
    val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."

    val program = for {
      _ <- db.Value.setup
      insertId <- db.Value.insert(text)
      selectId <- sql"select id from value".query[String].unique
    } yield (insertId, selectId)

    val (insertId, selectId) = transactor.use(program.transact).unsafeRunSync()

    assert(insertId == selectId)
  }

  it("should insert idempotently") {
    val program = for {
      _ <- db.Value.setup
      insertId <- db.Value.insert("foo")
      ignoreId <- db.Value.insert("foo")
      selectId <- sql"select id from value".query[String].unique
    } yield (insertId, ignoreId, selectId)

    val (insertId, ignoreId, selectId) = transactor.use(program.transact).unsafeRunSync()

    assert(insertId == ignoreId)
    assert(insertId == selectId)
  }
}
