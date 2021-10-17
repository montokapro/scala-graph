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

  val setupTenant =
    sql"""
      CREATE TABLE tenant (
        source INT,
        name STRING
      )
    """.update.run

  val setupValue =
    sql"""
      CREATE TABLE value (
        id STRING,
        value STRING
      )
    """.update.run

  val setupTenantValue =
    sql"""
      CREATE TABLE tenant_value (
        tenant_id STRING,
        value_id STRING
      )
    """.stripMargin

  val setupEdge =
    sql"""
      CREATE TABLE edge (
        source INT,
        target INT
      )
    """.update.run

  def insert(source: Int, target: Int): Update0 =
    sql"insert into edge (source, target) values ($source, $target)".update

  Tree.test(Tree.example)

  override def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      for {
        _ <- setupEdge.transact(xa)
        _ <- insert(1, 2).run.transact(xa)
        n <- sql"SELECT 'Hello World!' FROM edge".query[String].unique.transact(xa)
        _ <- IO(println(n))
      } yield ExitCode.Success
    }
}