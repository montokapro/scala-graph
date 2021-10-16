import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.h2._

object Main extends IOApp {
  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, H2Transactor[IO]] = {
    val url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"

    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- H2Transactor.newH2Transactor[IO](url, "username", "password", ec)
    } yield xa
  }

  override def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      for {
        n <- sql"select 'Hello World!'".query[String].unique.transact(xa)
        _ <- IO(println(n))
      } yield ExitCode.Success
    }
}