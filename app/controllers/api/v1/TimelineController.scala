package controllers.api.v1

import play.api.libs.json.*
import play.api.mvc.*
import repositories.StatusRepository
import security.AuthAction

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TimelineController @Inject() (
    authAction: AuthAction,
    cc: ControllerComponents,
    statusRepo: StatusRepository
)(using ExecutionContext)
    extends AbstractController(cc) {
  def home(
      max_id: Option[String],
      since_id: Option[String],
      min_id: Option[String],
      limit: Option[Int]
  ): Action[AnyContent] =
    authAction().async { request =>
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
