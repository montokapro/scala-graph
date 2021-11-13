import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._
import doobie.h2._
import fs2._

class EventLoopSpec extends AnyFunSpec {
  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  def transactor: Resource[IO, Transactor[IO]] = {
    val databaseName = scala.util.Random.alphanumeric.take(16).mkString

    val url = s"jdbc:h2:mem:$databaseName;MODE=PostgreSQL"

    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](1)
      xa <- H2Transactor.newH2Transactor[IO](url, "username", "password", ec)
    } yield xa
  }

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
    val program = transactor.use { xa =>
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
