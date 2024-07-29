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
      max_id: Option[String],
      since_id: Option[String],
      min_id: Option[String],
      limit: Option[Int]
  ): Action[AnyContent] =
    authActionDB() { request =>
      statusRepo
        .timeline(
          statusRepo.TimelineType.Home(request.userId),
          limit.getOrElse(20),
          since_id.flatMap(_.toLongOption),
          max_id.flatMap(_.toLongOption)
        )
        .map { statuses =>
          Ok(Json.toJson(statuses))
        }
    }

}
