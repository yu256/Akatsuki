package models

import play.api.libs.json.*
import play.api.libs.json.JsonNaming.SnakeCase
import repositories.Tables

import java.time.ZonedDateTime

case class Status(
    id: String,
    createdAt: ZonedDateTime,
    inReplyToId: Option[String] = None,
    inReplyToAccountId: Option[String] = None,
    sensitive: Boolean = false,
    spoilerText: String = "",
    visibility: String,
    language: Option[String] = None,
    uri: String = "",
    url: Option[String] = None,
    repliesCount: Int = 0,
    reblogsCount: Int = 0,
    favouritesCount: Int = 0,
    favourited: Boolean = false,
    content: String,
    account: Account,
    mediaAttachments: Seq[MediaAttachment] = Seq.empty,
    mentions: Seq[Mention] = Seq.empty,
    tags: Seq[Tag] = Seq.empty,
    emojis: Seq[CustomEmoji] = Seq.empty
)

object Status {
  implicit val StatusFormat: OFormat[Status] = Json.format[Status]
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)

  def fromRow(
      s: Tables.StatusesRow,
      a: Tables.AccountsRow,
      mSeq: Seq[Tables.MediaRow]
  ): Status =
    Status(
      id = s.id.toString,
      createdAt = s.createdAt,
      inReplyToId = s.inReplyToId.map(_.toString),
      inReplyToAccountId = None,
      sensitive = s.sensitive,
      spoilerText = s.spoilerText,
      visibility = s.visibility match {
        case 0 => "public"
        case 1 => "unlisted"
        case 2 => "private"
        case 3 => "direct"
      },
      content = s.text,
      mediaAttachments = mSeq.map(MediaAttachment.fromRow),
      account = Account.fromRow(a)
    )
}

case class Mention(
    id: String,
    username: String,
    url: String,
    acct: String
)

object Mention {
  implicit val MentionFormat: OFormat[Mention] = Json.format[Mention]
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)
}

case class Tag(
    name: String,
    url: String
)

object Tag {
  implicit val TagFormat: OFormat[Tag] = Json.format[Tag]
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)
}
