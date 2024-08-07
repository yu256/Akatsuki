package repositories

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import slick.dbio.DBIO

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait AuthRepository {
  def createToken(
      userId: Long,
      applicationId: Option[Long] = None,
      scopes: Option[String] = None
  ): DBIO[Tables.AccessTokensRow]

  def createApp(
      clientName: String,
      redirectUris: String,
      scopes: Option[String],
      website: Option[String]
  ): DBIO[Tables.ApplicationsRow]

  def genAppCode(
      clientId: Long
  ): DBIO[String]

  def findAppByApplicationId(
      applicationId: Long
  ): DBIO[Option[Tables.ApplicationsRow]]

  def saveOwnerId(
      clientId: Long,
      ownerId: Long
  ): DBIO[Int]

  def findUserByCode(
      code: String
  ): DBIO[Option[(Tables.ApplicationsRow, Tables.UsersRow)]]

  def findToken(token: String): DBIO[Option[Tables.AccessTokensRow]]
  def findToken(
      applicationId: Long,
      scopes: Option[String] = None,
      userId: Long
  ): DBIO[Option[Tables.AccessTokensRow]]

  def findUserByToken(token: String): DBIO[Option[Tables.UsersRow]]
}

@Singleton
class AuthRepositoryImpl @Inject() ()(using ExecutionContext)
    extends AuthRepository {
  import MyPostgresDriver.api.given

  private val bcrypt = new BCryptPasswordEncoder

  private inline def genUUID = java.util.UUID.randomUUID().toString

  def createToken(
      userId: Long,
      applicationId: Option[Long] = None,
      scopes: Option[String] = None
  ): DBIO[Tables.AccessTokensRow] =
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
  ): DBIO[Tables.ApplicationsRow] =
    sql"""
         INSERT INTO applications (name, redirect_uri, scopes, website, secret)
         VALUES ($clientName, $redirectUris, $scopes, $website, $genUUID)
         RETURNING *
       """
      .as[Tables.ApplicationsRow]
      .head

  def findAppByApplicationId(
      applicationId: Long
  ): DBIO[Option[Tables.ApplicationsRow]] =
    Tables.Applications
      .filter(_.id === applicationId)
      .result
      .headOption

  def genAppCode(
      clientId: Long
  ): DBIO[String] = {
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
  ): DBIO[Int] =
    sql"""
        UPDATE applications
        SET owner_id = $ownerId
        WHERE id = $clientId
     """.asUpdate

  def findUserByCode(
      code: String
  ): DBIO[Option[(Tables.ApplicationsRow, Tables.UsersRow)]] =
    Tables.Applications
      .filter(_.code === code)
      .join(Tables.Users)
      .on(_.ownerId === _.id)
      .result
      .headOption

  def findToken(token: String): DBIO[Option[Tables.AccessTokensRow]] =
    Tables.AccessTokens
      .filter(_.token === token)
      .result
      .headOption

  def findToken(
      applicationId: Long,
      scopes: Option[String] = None,
      userId: Long
  ): DBIO[Option[Tables.AccessTokensRow]] = {
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

  override def findUserByToken(token: String): DBIO[Option[Tables.UsersRow]] =
    (for {
      tokens <- Tables.AccessTokens if tokens.token === token
      users <- Tables.Users if users.id === tokens.resourceOwnerId
    } yield users).result.headOption
}
