package controllers.api.v1

import cats.data.EitherT
import cats.syntax.all.*
import models.Account
import org.postgresql.util.PSQLException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import play.api.data.Form
import play.api.data.Forms.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.libs.json.Json.JsValueWrapper
import play.api.mvc.*
import repositories.{AccountRepository, AuthRepository, UserRepository}
import scalaoauth2.provider.InvalidRequest
import security.AuthController
import slick.dbio.DBIO

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AccountsController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    accountRepo: AccountRepository,
    userRepo: UserRepository
)(using ExecutionContext)
    extends AuthController(authRepo, cc, dbConfigProvider) {
  import AccountsController.*
  import extensions.FunctionalDBIO.{asEither, given}

  def register(redirect: Option[String]): Action[RegisterRequest] =
    ActionDB(parse.form(userForm)) { request =>
      val bcrypt = BCryptPasswordEncoder()
      val dbAction = for {
        accountId <-
          accountRepo.create(
            username = request.body.username,
            displayName = request.body.username
          )
        userId <-
          userRepo.create(
            email = request.body.email,
            encryptedPassword = bcrypt.encode(request.body.password),
            accountId = accountId
          )
        tokenRow <-
          authRepo.createToken(
            userId = userId,
            scopes = "read write follow push".some
          )
      } yield userId -> tokenRow.token

      import repositories.MyPostgresDriver.MyAPI.jdbcActionExtensionMethods

      val token: EitherT[DBIO, Throwable, String] = for {
        _ <- EitherT.fromEither[DBIO](validateRegisterRequest(request.body))
        (userId, token) <- EitherT(dbAction.transactionally.asEither)
        _ <- EitherT.liftF {
          request.session
            .get("clientId")
            .flatMap(_.toLongOption)
            .traverse_(authRepo.saveOwnerId(_, userId))
        }
      } yield token

      token.fold(
        {
          case ex: InvalidRequest =>
            BadRequest(Json.obj("error" -> ex.description))
          case ex: PSQLException
              if ex.getSQLState == "23505" /* Unique constraint violation */ =>
            BadRequest(Json.obj("error" -> ex.getMessage))
          case ex => InternalServerError(Json.obj("error" -> ex.getMessage))
        },
        accessToken =>
          redirect.fold(
            Ok(
              Json.obj(
                "access_token" -> accessToken,
                "token_type" -> "Bearer",
                "scope" -> "read write follow push"
              )
            )
          )(Redirect(_))
      )
    }

  private def validateRegisterRequest(
      request: RegisterRequest
  ): Either[InvalidRequest, Unit] = {
    val cond = Either.cond[String, Unit](_, (), _)

    cond(
      request.username.nonEmpty,
      "Username cannot be empty"
    ) >> cond(
      request.password.nonEmpty,
      "Password cannot be empty"
    ) >> cond(
      request.username.matches("^[a-zA-Z0-9_]+$"),
      "Username must contain only letters, numbers and underscores"
    ) >> cond(request.agreement, "Agreement must be accepted") >> cond(
      request.locale.forall(_.matches("^[a-zA-Z]+$")),
      "Locale must contain only letters"
    ) >> cond(
      request.reason.forall(_.length <= 200),
      "Reason must be at most 200 characters long"
    ) leftMap (InvalidRequest(_))
  }

  val verify: Action[AnyContent] =
    authAction().async { request =>
      runM(
        accountRepo
          .findByUserId(request.userId)
      )
        .map(Account.fromRow)
        .fold(InternalServerError(Json.obj("error" -> "Account not found"))) {
          account => Ok(Json.toJson(account))
        }
    }

  def getAccount(id: String): Action[AnyContent] = ActionDB() { _ =>
    (for {
      accountId <- EitherT.fromEither[DBIO](
        id.toLongOption.toRight(
          InvalidRequest("Invalid account ID")
        )
      )
      account <- EitherT(accountRepo.findByAccountId(accountId).asEither)
    } yield account.map(Account.fromRow)).fold(
      {
        case e: InvalidRequest => BadRequest(Json.obj("error" -> e.description))
        case e => InternalServerError(Json.obj("error" -> e.getMessage))
      },
      _.fold(NotFound(Json.obj("error" -> "Account not found"))) { account =>
        Ok(Json.toJson(account))
      }
    )
  }
}

object AccountsController {
  case class RegisterRequest(
      username: String,
      email: Option[String],
      password: String,
      agreement: Boolean,
      locale: Option[String],
      reason: Option[String]
  )

  val userForm: Form[RegisterRequest] = Form(
    mapping(
      "username" -> nonEmptyText,
      "email" -> optional(email),
      "password" -> nonEmptyText(minLength = 6),
      "agreement" -> boolean
        .verifying(
          "You must agree to the terms and conditions.",
          identity[Boolean]
        ),
      "locale" -> optional(text),
      "reason" -> optional(text)
    )(RegisterRequest.apply)(d =>
      Some((d.username, d.email, d.password, d.agreement, d.locale, d.reason))
    )
  )
}
