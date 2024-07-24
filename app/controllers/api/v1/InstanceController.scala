package controllers.api.v1

import play.api.Configuration
import play.api.libs.json.*
import play.api.mvc.*

import javax.inject.Inject

class InstanceController @Inject (
    cc: ControllerComponents,
    config: Configuration
) extends AbstractController(cc) {
  // wip
  val instance: Action[AnyContent] = Action {
    Function.const(
      Ok(
        Json.obj(
          "uri" -> config.get[String]("app.url"),
          "title" -> "Akatsuki",
          "short_description" -> "",
          "description" -> "",
          "email" -> "",
          "version" -> "0.0.1",
          "urls" -> Json.obj(
            "streaming_api" -> ""
          ),
          "stats" -> Json.obj(
            "user_count" -> 0,
            "status_count" -> 0,
            "domain_count" -> 0
          ),
          "thumbnail" -> JsNull,
          "languages" -> Json.arr("ja"),
          "registrations" -> true,
          "approval_required" -> false,
          "invites_enabled" -> false,
          "configuration" -> Json.obj(
            "statuses" -> Json.obj(
              "max_characters" -> 500,
              "max_media_attachments" -> 4,
              "characters_reserved_per_url" -> 23
            ),
            "media_attachments" -> Json.obj(
              "supported_mime_types" -> Json.arr(
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp",
                "video/webm",
                "video/mp4",
                "video/quicktime",
                "video/ogg",
                "audio/wave",
                "audio/wav",
                "audio/x-wav",
                "audio/x-pn-wave",
                "audio/vnd.wave",
                "audio/ogg",
                "audio/vorbis",
                "audio/mpeg",
                "audio/mp3",
                "audio/webm",
                "audio/flac",
                "audio/aac",
                "audio/m4a",
                "audio/x-m4a",
                "audio/mp4",
                "audio/3gpp",
                "video/x-ms-asf"
              ),
              "image_size_limit" -> 10485760,
              "image_matrix_limit" -> 16777216,
              "video_size_limit" -> 41943040,
              "video_frame_rate_limit" -> 60,
              "video_matrix_limit" -> 2304000
            ),
            "polls" -> Json.obj(
              "max_options" -> 4,
              "max_characters_per_option" -> 50,
              "min_expiration" -> 300,
              "max_expiration" -> 2629746
            )
          ),
          "contact_account" -> JsNull,
          "rules" -> Json.arr()
        )
      )
    )
  }
}
