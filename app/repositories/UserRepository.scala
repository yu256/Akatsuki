package repositories

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait UserRepository {
  def create(
      email: Option[String] = None,
      encryptedPassword: String,
      accountId: Long
  ): Future[Long]
  def findByEmail(email: String): Future[Option[Tables.UsersRow]]
  def findById(id: Long): Future[Option[Tables.UsersRow]]
}

@Singleton
class UserRepositoryImpl @Inject() (dbConfigProvider: DatabaseConfigProvider)(
    using ExecutionContext
) extends UserRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

  import MyPostgresDriver.api.*
  import dbConfig.*

  def create(
      email: Option[String] = None,
      encryptedPassword: String,
      accountId: Long
  ): Future[Long] = db.run {
    sql"""
         INSERT INTO users (email, encrypted_password, account_id)
         VALUES ($email, $encryptedPassword, $accountId)
         RETURNING id
       """
      .as[Long]
      .head
  }

  def findByEmail(email: String): Future[Option[Tables.UsersRow]] =
    db.run(Tables.Users.filter(_.email === email).result.headOption)

  def findById(id: Long): Future[Option[Tables.UsersRow]] =
    db.run(Tables.Users.filter(_.id === id).result.headOption)
}
