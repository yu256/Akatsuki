package controllers.api.v1

import cats.data.{EitherT, OptionT}
import cats.syntax.all.*
import models.Account
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.libs.json.Json.JsValueWrapper
import play.api.mvc.*
import repositories.{AccountRepository, AuthRepository, UserRepository}
import security.AuthAction

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccountsController @Inject() (
    authAction: AuthAction,
    cc: ControllerComponents,
    accountRepo: AccountRepository,
    userRepo: UserRepository,
    authRepo: AuthRepository
)(using ExecutionContext)
    extends AbstractController(cc) {
  import AccountsController.*

  // todo: run db transactional
  def register(redirect: Option[String]): Action[RegisterRequest] =
    Action.async(parse.form(userForm)) { request =>
      extension (opt: Option[?]) {
        private def ensureNone(msg: String): Either[String, Unit] =
          opt.fold(Right(()))(_ => Left(msg))
      }

      val bcrypt = BCryptPasswordEncoder()
      val accessTokenEitherT = for {
        _ <- EitherT.fromEither[Future](validateRegisterRequest(request.body))
        _ <- EitherT(
          (
            accountRepo
              .findByUsername(request.body.username, None)
              .map(_.ensureNone("username is duplicated.")),
            request.body.email.traverse {
              userRepo
                .findByEmail(_)
                .map(_.ensureNone("email is duplicated."))
            }
          ).mapN(_ *> _.getOrElse(Right(())))
        )
        accountId <- EitherT.liftF(
          accountRepo.create(
            username = request.body.username,
            displayName = request.body.username
          )
        )
        userId <- EitherT.liftF(
          userRepo.create(
            email = request.body.email,
            encryptedPassword = bcrypt.encode(request.body.password),
            accountId = accountId
          )
        )
        tokenRow <- EitherT.liftF(
          authRepo.createToken(
            userId = userId,
            scopes = "read write follow push".some
          )
        )
        _ <- EitherT.liftF {
          request.session
            .get("clientId")
            .flatMap(_.toLongOption)
            .traverse_(authRepo.saveOwnerId(_, userId))
        }
      } yield tokenRow.token

      accessTokenEitherT.fold(
        error => BadRequest(Json.obj("error" -> error)),
        accessToken =>
          redirect match {
            case Some(url) => Redirect(url)
            case None =>
              Ok(
                Json.obj(
                  "access_token" -> accessToken,
                  "token_type" -> "Bearer",
                  "scope" -> "read write follow push"
                )
              )
          }
      )
    }

  private def validateRegisterRequest(
      request: RegisterRequest
  ): Either[String, Unit] = {
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
    )
  }

  val verify: Action[AnyContent] =
    authAction().async { request =>
      OptionT {
        accountRepo
          .findByUserId(request.userId)
      }
        .map(Account.fromRow)
        .fold(InternalServerError(Json.obj("error" -> "Account not found"))) {
          account => Ok(Json.toJson(account))
        }
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
