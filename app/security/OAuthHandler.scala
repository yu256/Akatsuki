package security

import cats.data.OptionT
import cats.syntax.all.*
import repositories.{AuthRepository, Tables, UserRepository}
import scalaoauth2.provider.*
import slick.dbio.DBIO

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OAuthHandler @Inject() (
    authRepo: AuthRepository,
    userRepo: UserRepository
)(using ExecutionContext)
    extends DataHandler[Tables.UsersRow] {
  import scala.util.chaining.scalaUtilChainingOps
  import extensions.functionalDBIO.given 

  override def validateClient(
      maybeCredential: Option[ClientCredential],
      request: AuthorizationRequest
  ): Future[Boolean] =
    (for {
      credential <- OptionT.fromOption[Future](maybeCredential)
      (clientId, clientSecret) <- OptionT.fromOption[Future](
        (credential.clientId.toLongOption, credential.clientSecret).tupled
      )
      app <- OptionT(authRepo.run(authRepo.findAppByApplicationId(clientId)))
    } yield app.secret == clientSecret).getOrElse(false)

  private def toAccessToken(tokenRow: Tables.AccessTokensRow) =
    AccessToken(
      tokenRow.token,
      None,
      tokenRow.scopes,
      None,
      java.util.Date.from(tokenRow.createdAt.toInstant),
      Map("created_at" -> tokenRow.createdAt.toEpochSecond.toString)
    )

  override def getStoredAccessToken(
      authInfo: AuthInfo[Tables.UsersRow]
  ): Future[Option[AccessToken]] =
    authInfo.clientId.flatMap(_.toLongOption).flatTraverse { id =>
      authRepo.run {
        authRepo
          .findToken(id, authInfo.scope, authInfo.user.id)
          .map(_.map(toAccessToken))
      }
    }

  override def createAccessToken(
      authInfo: AuthInfo[Tables.UsersRow]
  ): Future[AccessToken] = (
    for {
      clientId <- OptionT.fromOption[DBIO](
        authInfo.clientId >>= { _.toLongOption }
      )
      app <- OptionT(authRepo.findAppByApplicationId(clientId))
      tokenRow <- OptionT(
        app.ownerId.traverse(
          authRepo.createToken(_, app.id.some, app.scopes.some)
        )
      )
    } yield {
      toAccessToken(tokenRow)
    }
  ).getOrElse(throw InvalidClient()) pipe authRepo.run

  override def findUser(
      maybeCredential: Option[ClientCredential],
      request: AuthorizationRequest
  ): Future[Option[Tables.UsersRow]] =
    (for {
      credential <- OptionT.fromOption[DBIO](maybeCredential)
      (clientId, clientSecret) <- OptionT.fromOption[DBIO](
        (credential.clientId.toLongOption, credential.clientSecret).tupled
      )
      app <- OptionT(authRepo.findAppByApplicationId(clientId))
      if app.secret == clientSecret
      user <- OptionT(app.ownerId flatTraverse userRepo.findById)
    } yield user).value pipe authRepo.run

  override def findAuthInfoByRefreshToken(
      refreshToken: String
  ): Future[Option[AuthInfo[Tables.UsersRow]]] = ???

  override def refreshAccessToken(
      authInfo: AuthInfo[Tables.UsersRow],
      refreshToken: String
  ): Future[AccessToken] = ???

  override def findAuthInfoByCode(
      code: String
  ): Future[Option[AuthInfo[Tables.UsersRow]]] = authRepo.run {
    authRepo
      .findUserByCode(code)
      .map(
        _.map { (app, user) =>
          AuthInfo(
            user,
            app.id.toString.some,
            app.scopes.some,
            app.redirectUri.some
          )
        }
      )
  }

  override def deleteAuthCode(code: String): Future[Unit] =
    // do nothing
    ().pure

  override def findAccessToken(token: String): Future[Option[AccessToken]] =
    authRepo.run {
      authRepo
        .findToken(token)
        .map(_.map(toAccessToken))
    }

  override def findAuthInfoByAccessToken(
      accessToken: AccessToken
  ): Future[Option[AuthInfo[Tables.UsersRow]]] =
    (for {
      tokenRow <- OptionT(authRepo.findToken(accessToken.token))
      app <- OptionT(
        authRepo.findAppByApplicationId(tokenRow.applicationId.get)
      )
      user <- OptionT(userRepo.findById(tokenRow.resourceOwnerId))
    } yield {
      AuthInfo(
        user,
        app.id.toString.some,
        app.scopes.some,
        app.redirectUri.some
      )
    }).value pipe authRepo.run

}
