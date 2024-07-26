package repositories

import extensions.DBIOA
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait AuthRepository extends Repository {
  def createToken(
      userId: Long,
      applicationId: Option[Long] = None,
      scopes: Option[String] = None
  ): DBIOA[Tables.AccessTokensRow]

  def createApp(
      clientName: String,
      redirectUris: String,
      scopes: Option[String],
      website: Option[String]
  ): DBIOA[Tables.ApplicationsRow]

  def genAppCode(
      clientId: Long
  ): DBIOA[String]

  def findAppByApplicationId(
      applicationId: Long
  ): DBIOA[Option[Tables.ApplicationsRow]]

  def saveOwnerId(
      clientId: Long,
      ownerId: Long
  ): DBIOA[Int]

  def findUserByCode(
      code: String
  ): DBIOA[Option[(Tables.ApplicationsRow, Tables.UsersRow)]]

  def findToken(token: String): DBIOA[Option[Tables.AccessTokensRow]]
  def findToken(
      applicationId: Long,
      scopes: Option[String] = None,
      userId: Long
  ): DBIOA[Option[Tables.AccessTokensRow]]
}

@Singleton
class AuthRepositoryImpl @Inject (dbConfigProvider: DatabaseConfigProvider)(
    using ExecutionContext
) extends AuthRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

  import MyPostgresDriver.api.*
  import dbConfig.*

  val bcrypt = new BCryptPasswordEncoder

  private inline def genUUID = java.util.UUID.randomUUID().toString

  def run[T] = db.run[T]

  def createToken(
      userId: Long,
      applicationId: Option[Long] = None,
      scopes: Option[String] = None
  ): DBIOA[Tables.AccessTokensRow] =
    sql"""
         INSERT INTO access_tokens (resource_owner_id, token, application_id, scopes)
         VALUES ($userId, $genUUID, $applicationId, $scopes)
         RETURNING *
       """
      .as[Tables.AccessTokensRow]
      .head

  def createApp(
      clientName: String,
      redirectUris: String,
      scopes: Option[String],
      website: Option[String]
  ): DBIOA[Tables.ApplicationsRow] =
    sql"""
         INSERT INTO applications (name, redirect_uri, scopes, website, secret)
         VALUES ($clientName, $redirectUris, $scopes, $website, $genUUID)
         RETURNING *
       """
      .as[Tables.ApplicationsRow]
      .head

  def findAppByApplicationId(
      applicationId: Long
  ): DBIOA[Option[Tables.ApplicationsRow]] =
    Tables.Applications
      .filter(_.id === applicationId)
      .result
      .headOption

  def genAppCode(
      clientId: Long
  ): DBIOA[String] = {
    val code = genUUID

    sql"""
      UPDATE applications
      SET code = $code
      WHERE id = $clientId
    """.asUpdate.map(Function.const(code))
  }

  def saveOwnerId(
      clientId: Long,
      ownerId: Long
  ): DBIOA[Int] =
    sql"""
        UPDATE applications
        SET owner_id = $ownerId
        WHERE id = $clientId
     """.asUpdate

  def findUserByCode(
      code: String
  ): DBIOA[Option[(Tables.ApplicationsRow, Tables.UsersRow)]] =
    Tables.Applications
      .filter(_.code === code)
      .join(Tables.Users)
      .on(_.ownerId === _.id)
      .result
      .headOption

  def findToken(token: String): DBIOA[Option[Tables.AccessTokensRow]] =
    Tables.AccessTokens
      .filter(_.token === token)
      .result
      .headOption

  def findToken(
      applicationId: Long,
      scopes: Option[String] = None,
      userId: Long
  ): DBIOA[Option[Tables.AccessTokensRow]] = {
    val baseQuery =
      Tables.AccessTokens.filter(t =>
        t.applicationId === applicationId && t.resourceOwnerId === userId
      )

    scopes
      .fold(baseQuery) { s =>
        baseQuery.filter(t => t.scopes.map(_ === s))
      }
      .result
      .headOption
  }
}
