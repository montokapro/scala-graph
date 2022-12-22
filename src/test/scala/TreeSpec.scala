import org.scalatest.funspec.AnyFunSpec

import cats._
import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.implicits._
import doobie.implicits._
import scala.collection.immutable.ArraySeq

trait TreeInstances {
  implicit class ArrayOrdering[T: Ordering] extends Ordering[Array[T]] {
    def compare(x: Array[T], y: Array[T]) = {
      val xe = x.iterator
      val ye = y.iterator

      while (xe.hasNext && ye.hasNext) {
        val res = Ordering[T].compare(xe.next(), ye.next())
        if (res != 0) return res
      }

      Ordering[Boolean].compare(xe.hasNext, ye.hasNext)
    }
  }

  implicit val eqArray: Eq[Array[Byte]] = new Eq[Array[Byte]] {
    def eqv(x: Array[Byte], y: Array[Byte]): Boolean =
      ArraySeq.from(x) == ArraySeq.from(y)
  }

  implicit val eq: Eq[Tree] = new Eq[Tree] {
    def eqv(x: Tree, y: Tree): Boolean =
      Eq[Array[Byte]].eqv(x.value, y.value) && Eq[Vector[Tree]]
        .eqv(x.children.sortBy(_.value), y.children.sortBy(_.value))
  }
}

class TreeSpec extends AnyFunSpec with TreeInstances {
  def foo = "foo".getBytes("UTF-8")
  def bar = "bar".getBytes("UTF-8")
  def baz = "baz".getBytes("UTF-8")
  def fizz = "fizz".getBytes("UTF-8")
  def buzz = "buzz".getBytes("UTF-8")

  it("should insert and lookup") {
    val tree = Tree(
      foo,
      Vector(
        Tree(bar, Vector.empty),
        Tree(
          baz,
          Vector(
            Tree(fizz, Vector.empty),
            Tree(buzz, Vector.empty)
          )
        )
      )
    )

    val program = db.transactor.use { xa =>
      for {
        _ <- db.Edge.setup.transact(xa)
        _ <- Tree.insert(tree, xa)
        t <- Tree.lookup(foo, xa)
      } yield t
    }

    val output = program.unsafeRunSync()

    assert(eq.eqv(output, tree))
  }
}
