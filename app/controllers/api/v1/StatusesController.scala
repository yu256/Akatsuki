package controllers.api.v1

import play.api.data.Form
import play.api.data.Forms.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.mvc.*
import repositories.{AuthRepository, StatusRepository}
import security.AuthController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StatusesController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    statusRepo: StatusRepository
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
    }) { request =>
      val req = request.request.body

      statusRepo
        .createStatus(
          accountId = request.user.accountId,
          text = req.status,
          sensitive = req.sensitive,
          spoilerText = req.spoilerText,
          visibility = req.visibility.getOrElse(0),
          language = req.language,
          mediaIds =
            req.mediaIds.map(_.flatMap(_.toLongOption)).getOrElse(Seq.empty)
        )
        .map(Utils.toJsonResponse)
    }

  def delete(id: Long): Action[AnyContent] =
    authAction().async { request =>
      runM(statusRepo.deleteStatus(id, request.user.accountId))
        .fold(NotFound(Json.obj("error" -> "Record not found")))(
          Utils.toJsonResponse
        )
    }
}
