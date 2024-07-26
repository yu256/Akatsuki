package controllers.api.v1

import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.mvc.*
import repositories.StatusRepository
import security.{AuthAction, UserRequest}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StatusesController @Inject() (
    authAction: AuthAction,
    cc: ControllerComponents,
    statusRepo: StatusRepository
)(using
    ExecutionContext
) extends AbstractController(cc) {
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
    authAction(parse.form {
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
    }).async { case UserRequest(userId, request) =>
      val req = request.body
      statusRepo.run(
        statusRepo
          .createStatus(
            accountId = userId,
            text = req.status,
            sensitive = req.sensitive,
            spoilerText = req.spoilerText,
            visibility = req.visibility.getOrElse(0),
            language = req.language,
            mediaIds =
              req.mediaIds.map(_.flatMap(_.toLongOption)).getOrElse(Seq.empty)
          )
          .map(status => Ok(Json.toJson(status)))
      )
    }

  def delete(id: Long): Action[AnyContent] =
    authAction().async { case UserRequest(userId, _) =>
      statusRepo
        .runM(statusRepo.deleteStatus(id, userId))
        .fold(NotFound(Json.obj("error" -> "Record not found"))) { status =>
          Ok(Json.toJson(status))
        }
    }
}
