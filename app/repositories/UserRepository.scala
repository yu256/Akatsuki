package repositories

import extensions.DBIOA
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait UserRepository extends Repository {
  def create(
      email: Option[String] = None,
      encryptedPassword: String,
      accountId: Long
  ): DBIOA[Long]
  def findByEmail(email: String): DBIOA[Option[Tables.UsersRow]]
  def findById(id: Long): DBIOA[Option[Tables.UsersRow]]
}

@Singleton
class UserRepositoryImpl @Inject() (dbConfigProvider: DatabaseConfigProvider)(
    using ExecutionContext
) extends UserRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

  import MyPostgresDriver.api.*
  import dbConfig.*

  def run[T] = db.run[T]

  def create(
      email: Option[String] = None,
      encryptedPassword: String,
      accountId: Long
  ): DBIOA[Long] =
    sql"""
         INSERT INTO users (email, encrypted_password, account_id)
         VALUES ($email, $encryptedPassword, $accountId)
         RETURNING id
       """
      .as[Long]
      .head

  def findByEmail(email: String): DBIOA[Option[Tables.UsersRow]] =
    Tables.Users.filter(_.email === email).result.headOption

  def findById(id: Long): DBIOA[Option[Tables.UsersRow]] =
    Tables.Users.filter(_.id === id).result.headOption
}
