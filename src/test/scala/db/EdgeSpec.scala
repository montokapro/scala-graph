package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._

class EdgeSpec extends AnyFunSpec {
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

  it("should lookup outputs") {
    val program = for {
      _ <- db.Edge.setup
      _ <- db.Edge.insert("foo", "bar")
      _ <- db.Edge.insert("foo", "baz")
      outputs <- db.Edge.lookup("foo").compile.toList
    } yield outputs

    val outputs = transactor.use(program.transact).unsafeRunSync()

    assert(outputs.toSet == Set("bar", "baz"))
  }
}
