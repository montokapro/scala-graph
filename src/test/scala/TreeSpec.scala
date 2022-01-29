import org.scalatest.funspec.AnyFunSpec

import cats._
import cats.effect._
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.implicits._
import doobie.implicits._

trait TreeInstances {
  implicit val eq: Eq[Tree] = new Eq[Tree] {
    def eqv(x: Tree, y: Tree): Boolean =
      Eq[String].eqv(x.value, y.value) && Eq[Vector[Tree]]
        .eqv(x.children.sortBy(_.value), y.children.sortBy(_.value))
  }
}

class TreeSpec extends AnyFunSpec with TreeInstances {
  it("should insert and lookup") {
    val tree = Tree(
      "foo",
      Vector(
        Tree("bar", Vector.empty),
        Tree(
          "baz",
          Vector(
            Tree("fizz", Vector.empty),
            Tree("buzz", Vector.empty)
          )
        )
      )
    )

    val program = db.transactor.use { xa =>
      for {
        _ <- db.Edge.setup.transact(xa)
        _ <- Tree.insert(tree, xa)
        t <- Tree.lookup("foo", xa)
      } yield t
    }

    val output = program.unsafeRunSync()

    assert(eq.eqv(output, tree))
  }
}
