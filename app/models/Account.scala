package models

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.*
import repositories.Tables

import java.time.ZonedDateTime

case class Account(
    id: String,
    username: String,
    acct: String,
    displayName: String,
    locked: Boolean,
    bot: Boolean,
    discoverable: Option[Boolean] = None,
    group: Boolean = false,
    createdAt: Option[ZonedDateTime] = None,
    note: String,
    url: String,
    avatar: String = "",
    avatarStatic: String = "",
    header: String = "",
    headerStatic: String = "",
    followersCount: Int = 0,
    followingCount: Int = 0,
    statusesCount: Int = 0,
    lastStatusAt: Option[ZonedDateTime] = None,
    emojis: Seq[CustomEmoji] = Seq.empty,
    fields: Seq[Field] = Seq.empty
)

object Account {
  implicit val accountFormat: OFormat[Account] = Json.format[Account]
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)

  def fromRow(row: Tables.AccountsRow): Account =
    Account(
      id = row.id.toString,
      username = row.username,
      acct = row.domain match {
        case Some(domain) => s"${row.username}@$domain"
        case None         => row.username
      },
      displayName = row.displayName,
      locked = row.locked,
      bot = row.bot,
      note = row.note,
      url = row.url.getOrElse(""),
      createdAt = Some(row.createdAt)
    )
}

case class Field(name: String, value: String, verifiedAt: Option[String] = None)

object Field {
  implicit val FieldFormat: OFormat[Field] = Json.format[Field]
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)

}
