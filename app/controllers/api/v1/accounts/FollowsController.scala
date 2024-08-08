package controllers.api.v1.accounts

import play.api.data.Form
import play.api.data.Forms.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import repositories.{AuthRepository, FollowRepository}
import security.{AuthController, UserRequest}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FollowsController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    followRepo: FollowRepository
)(using ExecutionContext)
    extends AuthController(authRepo, cc, dbConfigProvider) {
  def follow(targetAccountId: Long): Action[FollowsController.FollowForm] =
    authActionDB(parse.form(FollowsController.followForm)) {
      case UserRequest(user, request) =>
        val dbAction = for {
          follow <- followRepo
            .follow(
              user.accountId,
              targetAccountId,
              request.body.reblogs,
              request.body.inform,
              request.body.languages
            )
          followed <- followRepo.getInfo(targetAccountId, user.accountId)
        } yield (follow, followed.isDefined)

        dbAction
          .map { (follow, isFollowed) =>
            Ok(
              Json.obj(
                "id" -> targetAccountId.toString,
                "following" -> true,
                "showing_reblogs" -> follow.showReblogs,
                "notifying" -> follow.inform,
                "followed_by" -> isFollowed,
                "blocking" -> false,
                "blocked_by" -> false,
                "muting" -> false,
                "muting_notifications" -> false,
                "requested" -> false,
                "domain_blocking" -> false,
                "endorsed" -> false
              )
            )
          }
    }
}

object FollowsController {
  case class FollowForm(
      reblogs: Boolean,
      inform: Boolean,
      languages: Seq[String]
  )

  private val followForm: Form[FollowForm] = Form(
    mapping(
      "reblogs" -> optional(boolean).transform(_.getOrElse(false), Some.apply),
      "notify" -> optional(boolean).transform(_.getOrElse(false), Some.apply),
      "languages" -> optional(seq(nonEmptyText))
        .transform(_.getOrElse(Seq.empty), Some.apply)
    )(FollowForm.apply)(d => Some((d._1, d._2, d._3)))
  )
}
