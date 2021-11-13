import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._
import fs2._

class EventLoopSpec extends AnyFunSpec {
  it("should enqueue and dequeue") {
    val queue: IO[Queue[IO, String]] = Queue.unbounded[IO, String]

    val program = for {
      q <- queue
      _ <- q.offer("foo")
      _ <- q.offer("bar")
      foo <- q.take
    } yield foo

    val foo = program.unsafeRunSync()

    assert(foo == "foo")
  }

  it("should trigger") {
    val program = db.transactor.use { xa =>
      val queue: IO[Queue[IO, String]] = Queue.unbounded[IO, String]

      val setup = for {
        _ <- db.Edge.setup
        _ <- db.Edge.insert("foo", "bar")
        _ <- db.Edge.insert("foo", "baz")
      } yield ()

      for {
        _ <- setup.transact(xa)
        q <- queue
        _ <- q.offer("foo")
        event <- q.take
        triggered <- db.Edge.lookup(event).compile.toList.transact(xa)
      } yield triggered

    }

    val outputs = program.unsafeRunSync()

    assert(outputs.toSet == Set("bar", "baz"))
  }
}
