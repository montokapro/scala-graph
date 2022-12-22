import org.scalatest.funspec.AnyFunSpec

import cats._
import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.implicits._
import doobie.implicits._
import scala.collection.immutable.ArraySeq

trait ByteArrayInstances {
  implicit val eq: Eq[Array[Byte]] = new Eq[Array[Byte]] {
    def eqv(x: Array[Byte], y: Array[Byte]): Boolean =
      ArraySeq.from(x) == ArraySeq.from(y)
  }
}

class EventLoopSpec extends AnyFunSpec with ByteArrayInstances {
  def not = "not".getBytes("UTF-8")
  def foo = "foo".getBytes("UTF-8")
  def bar = "bar".getBytes("UTF-8")
  def baz = "baz".getBytes("UTF-8")
  def fizz = "fizz".getBytes("UTF-8")
  def buzz = "buzz".getBytes("UTF-8")

  it("should enqueue and dequeue") {
    val queue: IO[Queue[IO, Array[Byte]]] = Queue.unbounded[IO, Array[Byte]]

    val program = for {
      q <- queue
      _ <- q.offer(foo)
      _ <- q.offer(bar)
      f <- q.take
    } yield f

    val f = program.unsafeRunSync()

    assert(ArraySeq.from(f) == ArraySeq.from(foo))
  }

  it("should trigger") {
    val program = db.transactor.use { xa =>
      val queue: IO[Queue[IO, Array[Byte]]] = Queue.unbounded[IO, Array[Byte]]

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

    assert(outputs.toSet.map(ArraySeq.from) == Set(bar, baz).map(ArraySeq.from))
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

      def handle(event: Array[Byte]): IO[Unit] = IO.unit // IO(println(event))

      def enqueue(queue: Queue[IO, Array[Byte]], input: Array[Byte]): IO[List[Array[Byte]]] = {
        for {
          outputs <- db.Edge.lookup(input).compile.toList.transact(xa)
          _ <- outputs.traverse(handle *> queue.offer)
        } yield outputs
      }

      def loop(queue: Queue[IO, Array[Byte]]): IO[List[Array[Byte]]] = {
        for {
          event <- queue.tryTake
          outputs <- event.fold(IO.pure(List.empty))(input =>
            handle(input) >> enqueue(queue, input) |+| loop(queue)
          )
        } yield outputs
      }

      for {
        _ <- setup.transact(xa)
        q <- Queue.unbounded[IO, Array[Byte]]
        _ <- q.offer(foo)
        outputs <- loop(q)
      } yield outputs
    }

    val outputs = program.unsafeRunSync()

    assert(outputs.toSet.map(ArraySeq.from) == Set(bar, baz, fizz, buzz).map(ArraySeq.from))
  }
}
