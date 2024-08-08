package controllers.api.v1

import cats.data.EitherT
import cats.syntax.all.*
import models.Account
import org.postgresql.util.PSQLException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import play.api.data.Form
import play.api.data.Forms.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.I18nSupport
import play.api.libs.json.*
import play.api.libs.json.Json.JsValueWrapper
import play.api.mvc.*
import repositories.*
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
    userRepo: UserRepository,
    statusRepo: StatusRepository,
    followRepo: FollowRepository
)(using ExecutionContext)
    extends AuthController(authRepo, cc, dbConfigProvider)
    with I18nSupport {
  import AccountsController.*
  import extensions.FunctionalDBIO.{asEither, given}

  import scala.util.chaining.scalaUtilChainingOps

  val register: Action[AnyContent] =
    ActionDB() { implicit request =>
      registerForm
        .bindFromRequest()
        .fold(
          formWithErrors => BadRequest(views.html.index(formWithErrors)).pure,
          { body =>
            val bcrypt = BCryptPasswordEncoder()
            val dbAction = for {
              accountId <-
                accountRepo.create(
                  username = body.username,
                  displayName = body.username
                )
              userId <-
                userRepo.create(
                  email = body.email,
                  encryptedPassword = bcrypt.encode(body.password),
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
              _ <- EitherT
                .fromEither[DBIO](validateRegisterRequest(body))
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
                  val formWithErrors =
                    if ex.getMessage.contains("username)=") then
                      registerForm
                        .withError("username", "Username already exists")
                    else
                      registerForm
                        .withError("email", "Email already exists")
                  BadRequest(views.html.index(formWithErrors))
                case ex => throw ex
              },
              accessToken =>
                request.session
                  .get("redirectTo")
                  .fold(
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

  private def getAccountFromDB(id: Long) =
    runM(
      accountRepo
        .findByAccountId(id)
    )
      .fold(BadRequest(Json.obj("error" -> "Account not found")))(
        Account.fromRow andThen Utils.toJsonResponse
      )

  val verify: Action[AnyContent] =
    authAction().async(_.user.accountId pipe getAccountFromDB)

  def getAccount(id: Long): Action[AnyContent] =
    Action.async(getAccountFromDB(id))

  def getUserTimeline(
      targetId: Long,
      onlyMedia: Boolean = false,
      maxId: Option[Long],
      sinceId: Option[Long],
      minId: Option[Long],
      limit: Int = 20
  ): Action[AnyContent] =
    optionalAuthActionDB() { request =>
      statusRepo
        .timeline(
          statusRepo.TimelineType
            .User(targetId, request.user.map(_.accountId)),
          limit,
          sinceId orElse minId,
          maxId
        )
        .map(Utils.toJsonResponse)
    }

  // wip
  def getRelationships(
      withSuspended: Boolean = false
  ): Action[AnyContent] = authActionDB() { request =>
    val ids: Seq[Long] = request.queryString
      .get("id[]")
      .flatTraverse(_.map(_.toLongOption))
      .flatten

    val infoF: DBIO[
      Seq[(Long, Option[Tables.FollowsRow], Option[Tables.FollowsRow])]
    ] =
      ids.map { id =>
        for {
          following <- followRepo.getInfo(request.user.accountId, id)
          follower <- followRepo.getInfo(id, request.user.accountId)
        } yield (id, following, follower)
      } pipe DBIO.sequence

    infoF.map { info =>
      Ok(Json.toJson {
        info.map { (id, following, follower) =>
          Json.obj(
            "id" -> id.toString,
            "followed_by" -> follower.isDefined,
            "blocking" -> false,
            "blocked_by" -> false,
            "muting" -> false,
            "muting_notifications" -> false,
            "requested" -> false,
            "domain_blocking" -> false,
            "endorsed" -> false
          ) ++
            following.fold(
              Json.obj(
                "following" -> false,
                "showing_reblogs" -> false,
                "notifying" -> false
              )
            ) { following =>
              Json.obj(
                "following" -> true,
                "showing_reblogs" -> following.showReblogs,
                "notifying" -> following.inform
              )
            }
        }
      })
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

  val registerForm: Form[RegisterRequest] = Form(
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
