package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import scala.collection.immutable.ArraySeq

class JunctionSpec extends AnyFunSpec {
  def foo = "foo".getBytes("UTF-8")
  def bar = "bar".getBytes("UTF-8")
  def baz = "baz".getBytes("UTF-8")

  it("should insert idempotently") {
    val program = for {
      _ <- db.Junction.setup
      key0 <- db.Junction.insert(Vector(foo))
      key1 <- db.Junction.insert(Vector(bar, baz))
      key2 <- db.Junction.insert(Vector(baz, bar))
    } yield (key0, key1, key2)

    val (key0, key1, key2) = transactor.use(program.transact).unsafeRunSync()

    assert(ArraySeq.from(key0) != ArraySeq.from(key1))
    assert(ArraySeq.from(key1) == ArraySeq.from(key2))
  }

  it("should loookup values") {
    val program = for {
      _ <- db.Junction.setup
      _ <- db.Junction.insert(Vector(foo))
      key <- db.Junction.insert(Vector(bar, baz))
      values <- db.Junction.lookup(key).compile.toList
    } yield values

    val values = transactor.use(program.transact).unsafeRunSync()

    assert(values.toSet.map(ArraySeq.from) == Set(bar, baz).map(ArraySeq.from))
  }
}
