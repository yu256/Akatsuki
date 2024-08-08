package controllers.api.v1

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import repositories.AuthRepository
import security.CustomController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AppsController @Inject() (
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    authRepo: AuthRepository
)(using ExecutionContext)
    extends CustomController(cc, dbConfigProvider) {
  case class AppsRequest(
      clientName: String,
      redirectUris: String,
      scopes: Option[String],
      website: Option[String]
  )

  val apps: Action[AppsRequest] = ActionDB(parse.form {
    Form(
      mapping(
        "client_name" -> nonEmptyText,
        "redirect_uris" -> nonEmptyText,
        "scopes" -> optional(text),
        "website" -> optional(text)
      )(AppsRequest.apply)(r =>
        Some((r.clientName, r.redirectUris, r.scopes, r.website))
      )
    )
  }) { request =>
    val AppsRequest(clientName, redirectUris, scopes, website) = request.body

    authRepo
      .createApp(
        clientName,
        redirectUris,
        scopes,
        website
      )
      .map(app =>
        Ok(
          Json.obj(
            "id" -> app.id.toString,
            "name" -> app.name,
            "website" -> app.website,
            "redirect_uri" -> app.redirectUri,
            "client_id" -> app.id.toString,
            "client_secret" -> app.secret
          )
        )
      )

  }
}
