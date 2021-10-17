import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.h2._

object Main extends IOApp {
  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, H2Transactor[IO]] = {
    val url = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- H2Transactor.newH2Transactor[IO](url, "username", "password", ec)
    } yield xa
  }

  // Debugging - re-add after https://github.com/tpolecat/doobie/pull/1566/files
  // implicit val handler: LogHandler = LogHandler.jdkLogHandler

  override def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      for {
        _ <- List(db.Tenant.setup, db.Value.setup, db.TenantValue.setup).traverse(_.transact(xa))
        alice <- db.Tenant.insert("Alice").transact(xa)
        _ <- IO(println(alice))
        bob <- db.Tenant.insert("Bob").transact(xa)
        _ <- IO(println(bob))
        _ <- db.Value.insert("foo").transact(xa)
        _ <- db.Value.insert("bar").transact(xa)
        _ <- db.Value.insert("bar").transact(xa)
        _ <- db.Value.insert("baz").transact(xa)
        _ <- sql"select id, value from value".query[(String, String)].stream.transact(xa).map(println).compile.drain
      } yield ExitCode.Success
   }
}