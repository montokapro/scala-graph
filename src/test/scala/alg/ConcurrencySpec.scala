package alg;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import scala.collection.immutable.ArraySeq

class ConcurrencySpec extends AnyFunSpec {
  def foo = ArraySeq.unsafeWrapArray("foo".getBytes("UTF-8"))
  def bar = ArraySeq.unsafeWrapArray("bar".getBytes("UTF-8"))
  def baz = ArraySeq.unsafeWrapArray("baz".getBytes("UTF-8"))

  it("should insert uniquely and idempotently") {
    val program = db.transactor.use { xa =>
      for {
        _ <- db.Junction.setup.transact(xa)
        key0 <- alg.Concurrency(xa).insert(Vector(foo, bar))
        key1 <- alg.Concurrency(xa).insert(Vector(foo, bar, baz))
        key2 <- alg.Concurrency(xa).insert(Vector(baz, bar, foo))
      } yield (key0, key1, key2)
    }

    val (key0, key1, key2) = program.unsafeRunSync()

    assert(key0 != key1)
    assert(key1 == key2)
  }

  it("should lookup values") {
    val program = db.transactor.use { xa =>
      for {
        _ <- db.Junction.setup.transact(xa)
        _ <- alg.Concurrency(xa).insert(Vector(foo, bar))
        key <- alg.Concurrency(xa).insert(Vector(foo, bar, baz))
        values <- alg.Concurrency(xa).lookupValues(key).compile.toList
      } yield values
    }

    val values = program.unsafeRunSync()

    assert(values.toSet == Set(foo, bar, baz))
  }

  it("should lookup key") {
    val program = db.transactor.use { xa =>
      for {
        _ <- db.Junction.setup.transact(xa)
        key0 <- alg.Concurrency(xa).insert(Vector(foo, bar))
        key1 <- alg.Concurrency(xa).insert(Vector(foo, bar, baz))
        keys <- alg.Concurrency(xa).lookupKeys(bar).compile.toList
      } yield (key0, key1, keys)
    }

    val (key0, key1, keys) = program.unsafeRunSync()

    assert(keys.toSet == Set(key0, key1))
  }
}
