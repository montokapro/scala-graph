import db.ByteSeq
import cats._
import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._
import fs2._

case class Tree(value: ByteSeq, children: Vector[Tree])

// Not optimized for efficiency. Consider batching, transaction boundaries.
object Tree {
  def toEdges(parent: Tree): Stream[Pure, (ByteSeq, ByteSeq)] =
    Stream.emits(parent.children).flatMap { child =>
      Stream.emit((parent.value, child.value)) ++ toEdges(child)
    }

  def insert(parent: Tree, xa: Transactor[IO]): IO[Unit] = {
    toEdges(parent)
      .evalMap(edge => db.Edge.insert(edge._1, edge._2))
      .transact(xa)
      .compile
      .drain
  }

  def lookup(parent: ByteSeq, xa: Transactor[IO]): IO[Tree] = {
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
