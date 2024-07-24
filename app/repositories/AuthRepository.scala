package repositories

import cats.syntax.all.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait AuthRepository {
  def createToken(
      userId: Long,
      applicationId: Option[Long] = None,
      scopes: Option[String] = None
  ): Future[Tables.AccessTokensRow]

  def createApp(
      clientName: String,
      redirectUris: String,
      scopes: Option[String],
      website: Option[String]
  ): Future[Tables.ApplicationsRow]

  def genAppCode(
      clientId: Long
  ): Future[String]

  def findAppByApplicationId(
      applicationId: Long
  ): Future[Option[Tables.ApplicationsRow]]

  def saveOwnerId(
      clientId: Long,
      ownerId: Long
  ): Future[Int]

  def findUserByCode(
      code: String
  ): Future[Option[(Tables.ApplicationsRow, Tables.UsersRow)]]

  def findUserIdByBearer(
      bearer: String
  ): Future[Option[Long]]

  def findToken(token: String): Future[Option[Tables.AccessTokensRow]]
  def findToken(
      applicationId: Long,
      scopes: Option[String] = None,
      userId: Long
  ): Future[Option[Tables.AccessTokensRow]]
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

  def createToken(
      userId: Long,
      applicationId: Option[Long] = None,
      scopes: Option[String] = None
  ): Future[Tables.AccessTokensRow] =
    db.run {
      sql"""
         INSERT INTO access_tokens (resource_owner_id, token, application_id, scopes)
         VALUES ($userId, $genUUID, $applicationId, $scopes)
         RETURNING *
       """
        .as[Tables.AccessTokensRow]
        .head
    }

  def createApp(
      clientName: String,
      redirectUris: String,
      scopes: Option[String],
      website: Option[String]
  ): Future[Tables.ApplicationsRow] = db.run {
    sql"""
         INSERT INTO applications (name, redirect_uri, scopes, website, secret)
         VALUES ($clientName, $redirectUris, $scopes, $website, $genUUID)
         RETURNING *
       """
      .as[Tables.ApplicationsRow]
      .head
  }

  def findAppByApplicationId(
      applicationId: Long
  ): Future[Option[Tables.ApplicationsRow]] =
    db.run {
      Tables.Applications
        .filter(_.id === applicationId)
        .result
        .headOption
    }

  def genAppCode(
      clientId: Long
  ): Future[String] =
    val code = genUUID
    db.run {
      sql"""
            UPDATE applications
            SET code = $code
            WHERE id = $clientId
         """.asUpdate
    } as code

  def saveOwnerId(
      clientId: Long,
      ownerId: Long
  ): Future[Int] =
    db.run {
      sql"""
            UPDATE applications
            SET owner_id = $ownerId
            WHERE id = $clientId
         """.asUpdate
    }

  def findUserByCode(
      code: String
  ): Future[Option[(Tables.ApplicationsRow, Tables.UsersRow)]] =
    db.run {
      Tables.Applications
        .filter(_.code === code)
        .join(Tables.Users)
        .on(_.ownerId === _.id)
        .result
        .headOption
    }

  def findUserIdByBearer(bearer: String): Future[Option[Long]] =
    db.run {
      Tables.AccessTokens
        .filter(_.token === bearer.substring(7))
        .map(_.resourceOwnerId)
        .result
        .headOption
    }

  def findToken(token: String): Future[Option[Tables.AccessTokensRow]] =
    db.run {
      Tables.AccessTokens
        .filter(_.token === token)
        .result
        .headOption
    }

  def findToken(
      applicationId: Long,
      scopes: Option[String] = None,
      userId: Long
  ): Future[Option[Tables.AccessTokensRow]] =
    db.run {
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
