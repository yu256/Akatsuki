package models

import play.api.libs.json.*
import play.api.libs.json.JsonNaming.SnakeCase
import repositories.Tables

case class MediaMeta(
    original: Option[MediaMetaDetails] = None,
    small: Option[MediaMetaDetails] = None,
    focus: Option[Focus] = None,
    length: Option[String] = None,
    duration: Option[Double] = None,
    fps: Option[Int] = None,
    size: Option[String] = None,
    width: Option[Int] = None,
    height: Option[Int] = None,
    aspect: Option[Double] = None,
    audioEncode: Option[String] = None,
    audioBitrate: Option[String] = None,
    audioChannels: Option[String] = None
)

case class MediaMetaDetails(
    width: Int,
    height: Int,
    size: String,
    aspect: Double,
    frameRate: Option[String] = None,
    duration: Option[Double] = None,
    bitrate: Option[Int] = None
)

case class Focus(
    x: Double,
    y: Double
)

case class MediaAttachment(
    id: String,
    `type`: String,
    url: String,
    previewUrl: Option[String] = None,
    remoteUrl: Option[String] = None,
    meta: Option[MediaMeta] = None,
    description: Option[String] = None,
    blurhash: Option[String] = None,
    textUrl: Option[String] = None
)

object MediaAttachment {
  implicit val focusFormat: Format[Focus] = Json.format[Focus]
  implicit val mediaMetaDetailsFormat: Format[MediaMetaDetails] =
    Json.format[MediaMetaDetails]
  implicit val mediaMetaFormat: Format[MediaMeta] = Json.format[MediaMeta]
  implicit val mediaAttachmentFormat: Format[MediaAttachment] =
    Json.format[MediaAttachment]

  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)

  def fromRow(row: Tables.MediaRow): MediaAttachment =
    MediaAttachment(
      id = row.id.toString,
      `type` = row.contentType match {
        case Some("image/jpeg" | "image/png" | "image/gif" | "image/webp") =>
          "image"
//        case Some("image/gif") => "gifv"
        case Some(
              "video/webm" | "video/mp4" | "video/quicktime" | "video/ogg" |
              "video/x-ms-asf"
            ) =>
          "video"
        case Some(
              "audio/wave" | "audio/wav" | "audio/x-wav" | "audio/x-pn-wave" |
              "audio/vnd.wave" | "audio/ogg" | "audio/vorbis" | "audio/mpeg" |
              "audio/mp3" | "audio/webm" | "audio/flac" | "audio/aac" |
              "audio/m4a" | "audio/x-m4a" | "audio/mp4" | "audio/3gpp"
            ) =>
          "audio"
        case _ => "unknown"
      },
      url = row.url
        .orElse(row.remoteUrl)
        .getOrElse(
          throw java.util.NoSuchElementException("url or remoteUrl is required")
        ),
      previewUrl = row.thumbnailUrl.orElse(row.url),
      remoteUrl = row.remoteUrl,
      meta = None,
      description = None,
      blurhash = Some(row.blurhash),
      textUrl = None
    )
}
