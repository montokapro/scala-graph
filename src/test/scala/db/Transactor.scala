package db;

import cats.effect._
import doobie._
import doobie.h2._

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
