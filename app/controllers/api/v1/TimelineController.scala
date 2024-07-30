package controllers.api.v1

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.mvc.*
import repositories.{AuthRepository, StatusRepository}
import security.AuthController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TimelineController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    statusRepo: StatusRepository
)(using ExecutionContext)
    extends AuthController(authRepo, cc, dbConfigProvider) {
  def home(
      max_id: Option[Long],
      since_id: Option[Long],
      min_id: Option[Long],
      limit: Int = 20
  ): Action[AnyContent] =
    authActionDB() { request =>
      statusRepo
        .timeline(
          statusRepo.TimelineType.Home(request.userId),
          limit,
          since_id orElse min_id,
          max_id
        )
        .map { statuses =>
          Ok(Json.toJson(statuses))
        }
    }

  def public(
      local: Boolean = false,
      remote: Boolean = false,
      only_media: Boolean = false,
      max_id: Option[Long],
      since_id: Option[Long],
      min_id: Option[Long],
      limit: Int = 20
  ): Action[AnyContent] =
    authActionDB() { request =>
      statusRepo
        .timeline(
          statusRepo.TimelineType.Public(local, remote, only_media),
          limit,
          since_id orElse min_id,
          max_id
        )
        .map { statuses =>
          Ok(Json.toJson(statuses))
        }
    }
}
