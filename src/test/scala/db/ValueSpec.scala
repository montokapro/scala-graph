package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import scala.collection.immutable.ArraySeq

class ValueSpec extends AnyFunSpec {
  def foo = ArraySeq.unsafeWrapArray("foo".getBytes("UTF-8"))
  def text = ArraySeq.unsafeWrapArray("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.".getBytes("UTF-8"))

  it("should insert long value") {
    val program = for {
      _ <- db.Value.setup
      insertId <- db.Value.insert(text)
      selectId <- sql"select id from value".query[ByteSeq].unique
    } yield (insertId, selectId)

    val (insertId, selectId) = transactor.use(program.transact).unsafeRunSync()

    assert(insertId == selectId)
  }

  it("should insert idempotently") {
    val program = for {
      _ <- db.Value.setup
      insertId <- db.Value.insert(foo)
      ignoreId <- db.Value.insert(foo)
      selectId <- sql"select id from value".query[ByteSeq].unique
    } yield (insertId, ignoreId, selectId)

    val (insertId, ignoreId, selectId) =
      transactor.use(program.transact).unsafeRunSync()

    assert(insertId == ignoreId)
    assert(insertId == selectId)
  }
}
