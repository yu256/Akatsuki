package controllers.oauth

import cats.data.EitherT
import cats.syntax.all.*
import controllers.routes
import play.api.i18n.I18nSupport
import play.api.libs.json.*
import play.api.mvc.*
import repositories.AuthRepository
import scalaoauth2.provider.*
import security.OAuthHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OAuth2Controller @Inject() (
    cc: ControllerComponents,
    authRepo: AuthRepository,
    oauthHandler: OAuthHandler
)(using
    ExecutionContext
) extends AbstractController(cc)
    with OAuth2Provider
    with I18nSupport {
  override val tokenEndpoint: TokenEndpoint = new TokenEndpoint {
    override val handlers: Map[String, GrantHandler] = Map(
      OAuthGrantType.AUTHORIZATION_CODE -> AuthorizationCode(),
      OAuthGrantType.REFRESH_TOKEN -> RefreshToken(),
      OAuthGrantType.CLIENT_CREDENTIALS -> ClientCredentials(),
      OAuthGrantType.PASSWORD -> Password(),
      OAuthGrantType.IMPLICIT -> Implicit()
    )
  }

  def authorize(
      response_type: String,
      client_id: String,
      redirect_uri: String,
      scope: Option[String],
      force_login: Option[Boolean],
      lang: Option[String]
  ): Action[AnyContent] = Action.async { request =>
    (for {
      clientId <- EitherT.fromOption[Future](
        client_id.toLongOption,
        "Invalid client_id"
      )
      app <- EitherT.fromOptionF(
        authRepo.run(authRepo.findAppByApplicationId(clientId)),
        "Invalid client_id"
      )
      result <- EitherT.liftF {
        if app.ownerId.isDefined then
          authRepo.run {
            authRepo.genAppCode(clientId).map { code =>
              redirect_uri match {
                case "urn:ietf:wg:oauth:2.0:oob" =>
                  Ok(views.html.auth_code(code))
                case uri => Redirect(s"$uri?code=$code")
              }
            }
          }
        else
          Redirect(
            routes.HomeController.index(
              request.headers.get("Raw-Request-URI")
            )
          ).withSession(
            "clientId" -> client_id
          ).pure[Future]
      }
    } yield {
      result
    }).valueOr(e => BadRequest(Json.obj("error" -> e)))
  }

  val token: Action[AnyContent] = Action.async { implicit request =>
    issueAccessToken(oauthHandler)
  }

  override def responseAccessToken[U](
      r: GrantHandlerResult[U]
  ): Map[String, JsValue] = {
    Map(
      "token_type" -> JsString(r.tokenType),
      "access_token" -> JsString(r.accessToken)
    ) ++ r.expiresIn.map {
      "expires_in" -> JsNumber(_)
    } ++ r.refreshToken.map {
      "refresh_token" -> JsString(_)
    } ++ r.scope.map {
      "scope" -> JsString(_)
    } ++ r.params.map { (k, v) =>
      k match {
        case "created_at" => k -> JsNumber(v.toLong)
        case _            => k -> JsString(v)
      }
    }
  }
}
