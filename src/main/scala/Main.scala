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

        aliceId <- db.Tenant.insert("Alice").transact(xa)
        bobId <- db.Tenant.insert("Bob").transact(xa)

        // Tenants may insert any value they like
        _ <- db.TenantValue.insertValue(aliceId, "foo").transact(xa)

        // Multiple tenants may insert the same value
        _ <- db.TenantValue.insertValue(aliceId, "bar").transact(xa)
        _ <- db.TenantValue.insertValue(bobId, "bar").transact(xa)

        // Values may be shared between tenants by reference
        bazId <- db.TenantValue.insertValue(aliceId, "baz").transact(xa)
        _ <- db.TenantValue.insert(bobId, bazId).transact(xa)

        // Long values are supported, and can be cached
        _ <- db.Value.insert("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.").transact(xa)

        _ <- IO(println("tenant"))
        _ <- sql"select id, name from tenant".query[(Int, String)].stream.transact(xa).map(println).compile.drain

        _ <- IO(println("tenant_value"))
        _ <- sql"select tenant_id, value_id from tenant_value".query[(Int, String)].stream.transact(xa).map(println).compile.drain

        _ <- IO(println("value"))
        _ <- sql"select id, value from value".query[(String, String)].stream.transact(xa).map(println).compile.drain
      } yield ExitCode.Success
   }
}