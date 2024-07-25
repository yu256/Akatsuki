package repositories

import extensions.DBIOA
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait AccountRepository extends Repository {
  def create(
      username: String,
      domain: Option[String] = None,
      displayName: String,
      locked: Boolean = false,
      bot: Boolean = false,
      note: String = "",
      url: Option[String] = None,
      fields: Option[String] = None
  ): DBIOA[Long]
  def findByUsername(
      username: String,
      domain: Option[String]
  ): DBIOA[Option[Tables.AccountsRow]]
  def findByUserId(userId: Long): DBIOA[Option[Tables.AccountsRow]]
}

@Singleton
class AccountRepositoryImpl @Inject() (
    dbConfigProvider: DatabaseConfigProvider
)(using
    ExecutionContext
) extends AccountRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

  import MyPostgresDriver.api.*
  import dbConfig.*

  def run[T] = db.run[T]

  // returns id
  def create(
      username: String,
      domain: Option[String] = None,
      displayName: String,
      locked: Boolean = false,
      bot: Boolean = false,
      note: String = "",
      url: Option[String] = None,
      fields: Option[String] = None
  ): DBIOA[Long] =
    sql"""
      INSERT INTO accounts (
        username, domain, display_name, locked, bot, note, url, fields
      ) VALUES (
        $username, $domain, $displayName, $locked, $bot,
        $note, $url, (to_jsonb($fields) #>> '{}')::jsonb
      ) RETURNING id
    """.as[Long].head

  def findByUsername(
      username: String,
      domain: Option[String]
  ): DBIOA[Option[Tables.AccountsRow]] =
    Tables.Accounts
      .filter(account =>
        account.username === username && (domain match {
          case Some(d) => account.domain === d
          case None    => account.domain.isEmpty.?
        })
      )
      .result
      .headOption

  def findByUserId(userId: Long): DBIOA[Option[Tables.AccountsRow]] =
    Tables.Users
      .filter(_.id === userId)
      .join(Tables.Accounts)
      .on(_.accountId === _.id)
      .map(_._2)
      .result
      .headOption

}
