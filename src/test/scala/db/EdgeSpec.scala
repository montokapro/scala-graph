package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import scala.collection.immutable.ArraySeq

class EdgeSpec extends AnyFunSpec {
  def foo = ArraySeq.unsafeWrapArray("foo".getBytes("UTF-8"))
  def bar = ArraySeq.unsafeWrapArray("bar".getBytes("UTF-8"))
  def baz = ArraySeq.unsafeWrapArray("baz".getBytes("UTF-8"))

  it("should insert idempotently") {
    val program = for {
      _ <- db.Edge.setup
      _ <- db.Edge.insert(foo, bar)
      _ <- db.Edge.insert(foo, bar)
      edge <- sql"select input, output from edge".query[(ByteSeq, ByteSeq)].unique
    } yield edge

    val (input, output) = transactor.use(program.transact).unsafeRunSync()

    assert(input == foo)
    assert(output == bar)
  }

  it("should lookup outputs") {
    val program = for {
      _ <- db.Edge.setup
      _ <- db.Edge.insert(foo, bar)
      _ <- db.Edge.insert(foo, baz)
      outputs <- db.Edge.lookup(foo).compile.toList
    } yield outputs

    val outputs = transactor.use(program.transact).unsafeRunSync()

    assert(outputs.toSet == Set(bar, baz))
  }
}
