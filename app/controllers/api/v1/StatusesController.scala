package controllers.api.v1

import play.api.data.Form
import play.api.data.Forms.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.mvc.*
import repositories.{AuthRepository, StatusRepository, UserRepository}
import security.{AuthController, UserRequest}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StatusesController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    statusRepo: StatusRepository,
    userRepo: UserRepository
)(using
    ExecutionContext
) extends AuthController(authRepo, cc, dbConfigProvider) {
  case class StatusRequest(
      status: String,
      mediaIds: Option[Seq[String]],
      poll: Option[PollRequest],
      inReplyToId: Option[String],
      sensitive: Boolean, // default false
      spoilerText: Option[String],
      visibility: Option[String],
      language: Option[String],
      scheduledAt: Option[String]
  )

  case class PollRequest(
      options: Seq[String],
      expiresIn: Int,
      multiple: Boolean, // default false
      hideTotals: Boolean // default false
  )

  private inline def getAccountId[A](using request: UserRequest[A]) =
    userRepo.findById(request.userId).map(_.get.accountId)

  val post: Action[StatusRequest] =
    authActionDB(parse.form {
      Form(
        mapping(
          "status" -> text,
          "media_ids" -> optional(seq(text)),
          "poll" -> optional(
            mapping(
              "options" -> seq(text),
              "expires_in" -> number,
              "multiple" -> optional(boolean)
                .transform[Boolean](_.getOrElse(false), Some.apply),
              "hide_totals" -> optional(boolean)
                .transform[Boolean](_.getOrElse(false), Some.apply)
            )(PollRequest.apply)(r =>
              Some(r.options, r.expiresIn, r.multiple, r.hideTotals)
            )
          ),
          "in_reply_to_id" -> optional(text),
          "sensitive" -> optional(boolean)
            .transform[Boolean](_.getOrElse(false), Some.apply),
          "spoiler_text" -> optional(text),
          "visibility" -> optional(text),
          "language" -> optional(text),
          "scheduled_at" -> optional(text)
        )(StatusRequest.apply)(r =>
          Some(
            r.status,
            r.mediaIds,
            r.poll,
            r.inReplyToId,
            r.sensitive,
            r.spoilerText,
            r.visibility,
            r.language,
            r.scheduledAt
          )
        )
      )
    }) { implicit request =>
      val req = request.request.body

      getAccountId
        .flatMap {
          statusRepo
            .createStatus(
              _,
              text = req.status,
              sensitive = req.sensitive,
              spoilerText = req.spoilerText,
              visibility = req.visibility.getOrElse(0),
              language = req.language,
              mediaIds =
                req.mediaIds.map(_.flatMap(_.toLongOption)).getOrElse(Seq.empty)
            )
        }
        .map(status => Ok(Json.toJson(status)))
    }

  def delete(id: Long): Action[AnyContent] =
    authAction().async { implicit request =>
      runM(getAccountId flatMap { statusRepo.deleteStatus(id, _) })
        .fold(NotFound(Json.obj("error" -> "Record not found"))) { status =>
          Ok(Json.toJson(status))
        }
    }
}
