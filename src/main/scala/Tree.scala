import cats._
import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._
import fs2._

case class Tree(value: String, children: Vector[Tree])

object Tree {
  def insert(parent: Tree, xa: Transactor[IO]): IO[Unit] = {
    parent.children.foldMap { child =>
      db.Edge.insert(parent.value, child.value).transact(xa) *> insert(
        child,
        xa
      )
    }
  }

  def lookup(parent: String, xa: Transactor[IO]): IO[Tree] = {
    db.Edge
      .lookup(parent)
      .transact(xa)
      .compile
      .toVector
      .flatMap { children =>
        children
          .traverse(child => lookup(child, xa))
          .map(Tree(parent, _))
      }
  }
}
