package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import scala.collection.immutable.ArraySeq

class JunctionSpec extends AnyFunSpec {
  def foo = ArraySeq.unsafeWrapArray("foo".getBytes("UTF-8"))
  def bar = ArraySeq.unsafeWrapArray("bar".getBytes("UTF-8"))
  def baz = ArraySeq.unsafeWrapArray("baz".getBytes("UTF-8"))

  it("should insert uniquely and idempotently") {
    val program = for {
      _ <- db.Junction.setup
      key0 <- db.Junction.insert(Vector(foo))
      key1 <- db.Junction.insert(Vector(bar, baz))
      key2 <- db.Junction.insert(Vector(baz, bar))
    } yield (key0, key1, key2)

    val (key0, key1, key2) = transactor.use(program.transact).unsafeRunSync()

    assert(key0 != key1)
    assert(key1 == key2)
  }

  it("should lookup values") {
    val program = for {
      _ <- db.Junction.setup
      _ <- db.Junction.insert(Vector(foo, bar))
      key <- db.Junction.insert(Vector(foo, bar, baz))
      values <- db.Junction.lookupValues(key).compile.toList
    } yield values

    val values = transactor.use(program.transact).unsafeRunSync()

    assert(values.toSet == Set(foo, bar, baz))
  }

  it("should lookup keys") {
    val program = for {
      _ <- db.Junction.setup
      key0 <- db.Junction.insert(Vector(foo, bar))
      key1 <- db.Junction.insert(Vector(foo, bar, baz))
      keys <- db.Junction.lookupKeys(bar).compile.toList
    } yield (key0, key1, keys)

    val (key0, key1, keys) = transactor.use(program.transact).unsafeRunSync()

    assert(keys.toSet == Set(key0, key1))
  }
}
