import org.scalatest.funspec.AnyFunSpec

import db._
import cats._
import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.implicits._
import doobie.implicits._
import scala.collection.immutable.ArraySeq

class EventLoopSpec extends AnyFunSpec {
  def not = ArraySeq.unsafeWrapArray("not".getBytes("UTF-8"))
  def foo = ArraySeq.unsafeWrapArray("foo".getBytes("UTF-8"))
  def bar = ArraySeq.unsafeWrapArray("bar".getBytes("UTF-8"))
  def baz = ArraySeq.unsafeWrapArray("baz".getBytes("UTF-8"))
  def fizz = ArraySeq.unsafeWrapArray("fizz".getBytes("UTF-8"))
  def buzz = ArraySeq.unsafeWrapArray("buzz".getBytes("UTF-8"))

  it("should enqueue and dequeue") {
    val queue: IO[Queue[IO, ByteSeq]] = Queue.unbounded[IO, ByteSeq]

    val program = for {
      q <- queue
      _ <- q.offer(foo)
      _ <- q.offer(bar)
      f <- q.take
    } yield f

    val f = program.unsafeRunSync()

    assert(f == foo)
  }

  it("should trigger") {
    val program = db.transactor.use { xa =>
      val queue: IO[Queue[IO, ByteSeq]] = Queue.unbounded[IO, ByteSeq]

      val setup = for {
        _ <- db.Edge.setup
        _ <- db.Edge.insert(foo, bar)
        _ <- db.Edge.insert(foo, baz)
      } yield ()

      for {
        _ <- setup.transact(xa)
        q <- queue
        _ <- q.offer(foo)
        event <- q.take
        triggered <- db.Edge.lookup(event).compile.toList.transact(xa)
      } yield triggered
    }

    val outputs = program.unsafeRunSync()

    assert(outputs.toSet == Set(bar, baz))
  }

  it("should traverse graph") {
    val program = db.transactor.use { xa =>
      val setup = for {
        _ <- db.Edge.setup
        _ <- db.Edge.insert(not, foo)
        _ <- db.Edge.insert(foo, bar)
        _ <- db.Edge.insert(foo, baz)
        _ <- db.Edge.insert(baz, fizz)
        _ <- db.Edge.insert(baz, buzz)
      } yield ()

      def handle(event: ByteSeq): IO[Unit] = IO.unit // IO(println(event))

      def enqueue(queue: Queue[IO, ByteSeq], input: ByteSeq): IO[List[ByteSeq]] = {
        for {
          outputs <- db.Edge.lookup(input).compile.toList.transact(xa)
          _ <- outputs.traverse(handle *> queue.offer)
        } yield outputs
      }

      def loop(queue: Queue[IO, ByteSeq]): IO[List[ByteSeq]] = {
        for {
          event <- queue.tryTake
          outputs <- event.fold(IO.pure(List.empty))(input =>
            handle(input) >> enqueue(queue, input) |+| loop(queue)
          )
        } yield outputs
      }

      for {
        _ <- setup.transact(xa)
        q <- Queue.unbounded[IO, ByteSeq]
        _ <- q.offer(foo)
        outputs <- loop(q)
      } yield outputs
    }

    val outputs = program.unsafeRunSync()

    assert(outputs.toSet == Set(bar, baz, fizz, buzz))
  }
}
