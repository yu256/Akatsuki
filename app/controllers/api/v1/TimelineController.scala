package controllers.api.v1

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.mvc.*
import repositories.{AuthRepository, StatusRepository, UserRepository}
import security.AuthController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TimelineController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    statusRepo: StatusRepository,
    userRepo: UserRepository
)(using ExecutionContext)
    extends AuthController(authRepo, cc, dbConfigProvider) {
  def home(
      maxId: Option[Long],
      sinceId: Option[Long],
      minId: Option[Long],
      limit: Int = 20
  ): Action[AnyContent] =
    authActionDB() { request =>
      userRepo
        .findById(request.userId)
        .flatMap { userAccountId =>
          statusRepo
            .timeline(
              statusRepo.TimelineType.Home(userAccountId.get.accountId),
              limit,
              sinceId orElse minId,
              maxId
            )
        }
        .map { statuses =>
          Ok(Json.toJson(statuses))
        }
    }

  def public(
      local: Boolean = false,
      remote: Boolean = false,
      onlyMedia: Boolean = false,
      maxId: Option[Long],
      sinceId: Option[Long],
      minId: Option[Long],
      limit: Int = 20
  ): Action[AnyContent] =
    ActionDB() { request =>
      statusRepo
        .timeline(
          statusRepo.TimelineType.Public(local, remote, onlyMedia),
          limit,
          sinceId orElse minId,
          maxId
        )
        .map { statuses =>
          Ok(Json.toJson(statuses))
        }
    }
}
