package repositories

import slick.dbio.DBIO

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait AccountRepository {
  def create(
      username: String,
      domain: Option[String] = None,
      displayName: String,
      locked: Boolean = false,
      bot: Boolean = false,
      note: String = "",
      url: Option[String] = None,
      fields: Option[String] = None
  ): DBIO[Long]
  def findByUsername(
      username: String,
      domain: Option[String]
  ): DBIO[Option[Tables.AccountsRow]]
  def findByAccountId(accountId: Long): DBIO[Option[Tables.AccountsRow]]
  def findByUserId(userId: Long): DBIO[Option[Tables.AccountsRow]]
}

@Singleton
class AccountRepositoryImpl @Inject() ()(using ExecutionContext)
    extends AccountRepository {
  import MyPostgresDriver.api.given

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
  ): DBIO[Long] =
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
  ): DBIO[Option[Tables.AccountsRow]] =
    Tables.Accounts
      .filter(account =>
        account.username === username && (domain match {
          case Some(d) => account.domain === d
          case None    => account.domain.isEmpty.?
        })
      )
      .result
      .headOption

  def findByAccountId(accountId: Long): DBIO[Option[Tables.AccountsRow]] =
    Tables.Accounts.filter(_.id === accountId).result.headOption

  def findByUserId(userId: Long): DBIO[Option[Tables.AccountsRow]] =
    Tables.Users
      .filter(_.id === userId)
      .join(Tables.Accounts)
      .on(_.accountId === _.id)
      .map(_._2)
      .result
      .headOption

}
