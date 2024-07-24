import scala.util.Properties.envOrElse

object Config {
  val url = envOrElse(
    "AKATSUKI_DB",
    throw RuntimeException("AKATSUKI_DB is not defined")
  )
  val jdbcDriver = "org.postgresql.Driver"
  val slickProfile: MyPostgresDriver = MyPostgresDriver
}
