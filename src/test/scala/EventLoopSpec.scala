import org.scalatest.funspec.AnyFunSpec

import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.implicits._
import doobie.implicits._

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

  it("should traverse graph") {
    val program = db.transactor.use { xa =>
      val setup = for {
        _ <- db.Edge.setup
        _ <- db.Edge.insert("not", "foo")
        _ <- db.Edge.insert("foo", "bar")
        _ <- db.Edge.insert("foo", "baz")
        _ <- db.Edge.insert("baz", "fizz")
        _ <- db.Edge.insert("baz", "buzz")
      } yield ()

      def handle(event: String): IO[Unit] = IO.unit // IO(println(event))

      def enqueue(queue: Queue[IO, String], input: String): IO[List[String]] = {
        for {
          outputs <- db.Edge.lookup(input).compile.toList.transact(xa)
          _ <- outputs.traverse(handle *> queue.offer)
        } yield outputs
      }

      def loop(queue: Queue[IO, String]): IO[List[String]] = {
        for {
          event <- queue.tryTake
          outputs <- event.fold(IO.pure(List.empty))(input =>
            handle(input) >> enqueue(queue, input) |+| loop(queue)
          )
        } yield outputs
      }

      for {
        _ <- setup.transact(xa)
        q <- Queue.unbounded[IO, String]
        _ <- q.offer("foo")
        outputs <- loop(q)
      } yield outputs
    }

    val outputs = program.unsafeRunSync()

    assert(outputs.toSet == Set("bar", "baz", "fizz", "buzz"))
  }
}
