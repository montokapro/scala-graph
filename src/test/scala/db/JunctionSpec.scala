package db;

import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits._

class JunctionSpec extends AnyFunSpec {
  it("should insert idempotently") {
    val program = for {
      _ <- db.Junction.setup
      key0 <- db.Junction.insert(Vector("foo"))
      key1 <- db.Junction.insert(Vector("bar", "baz"))
      key2 <- db.Junction.insert(Vector("baz", "bar"))
    } yield (key0, key1, key2)

    val (key0, key1, key2) = transactor.use(program.transact).unsafeRunSync()

    assert(key0 != key1)
    assert(key1 == key2)
  }

  it("should lookup values") {
    val program = for {
      _ <- db.Junction.setup
      _ <- db.Junction.insert(Vector("foo"))
      key <- db.Junction.insert(Vector("bar", "baz"))
      values <- db.Junction.lookup(key).compile.toList
    } yield values

    val values = transactor.use(program.transact).unsafeRunSync()

    assert(values.toSet == Set("bar", "baz"))
  }
}
