package controllers.oauth

import cats.syntax.all.*
import controllers.routes
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.I18nSupport
import play.api.libs.json.*
import play.api.mvc.*
import repositories.AuthRepository
import scalaoauth2.provider.*
import security.{CustomController, OAuthHandler}
import slick.dbio.DBIO

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class OAuth2Controller @Inject() (
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    authRepo: AuthRepository,
    oauthHandler: OAuthHandler
)(using
    ExecutionContext
) extends CustomController(cc, dbConfigProvider)
    with OAuth2Provider
    with I18nSupport {
  import extensions.FunctionalDBIO.given

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
      responseType: String,
      clientId: Long,
      redirectUri: String,
      scope: Option[String],
      forceLogin: Option[Boolean],
      lang: Option[String]
  ): Action[AnyContent] = ActionDB() { request =>
    authRepo.findAppByApplicationId(clientId).flatMap {
      _.fold(BadRequest(Json.obj("error" -> "Invalid client_id")).pure) { app =>
        if app.ownerId.isDefined then
          authRepo.genAppCode(clientId).map { code =>
            if redirectUri == "urn:ietf:wg:oauth:2.0:oob" then
              Ok(views.html.auth_code(code))
            else Redirect(s"$redirectUri?code=$code")
          }
        else
          Redirect(
            routes.HomeController.index
          ).withSession(
            "clientId" -> clientId.toString,
            "redirectTo" -> request.headers
              .get("Raw-Request-URI")
              .getOrElse(throw UnknownError("Failed to get Raw-Request-URI"))
          ).pure[DBIO]
      }
    }
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
