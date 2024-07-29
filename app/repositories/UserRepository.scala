package repositories

import slick.dbio.DBIO

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait UserRepository {
  def create(
      email: Option[String] = None,
      encryptedPassword: String,
      accountId: Long
  ): DBIO[Long]
  def findByEmail(email: String): DBIO[Option[Tables.UsersRow]]
  def findById(id: Long): DBIO[Option[Tables.UsersRow]]
}

@Singleton
class UserRepositoryImpl @Inject() ()(using ExecutionContext)
    extends UserRepository {
  import MyPostgresDriver.api.*

  def create(
      email: Option[String] = None,
      encryptedPassword: String,
      accountId: Long
  ): DBIO[Long] =
    sql"""
         INSERT INTO users (email, encrypted_password, account_id)
         VALUES ($email, $encryptedPassword, $accountId)
         RETURNING id
       """
      .as[Long]
      .head

  def findByEmail(email: String): DBIO[Option[Tables.UsersRow]] =
    Tables.Users.filter(_.email === email).result.headOption

  def findById(id: Long): DBIO[Option[Tables.UsersRow]] =
    Tables.Users.filter(_.id === id).result.headOption
}
