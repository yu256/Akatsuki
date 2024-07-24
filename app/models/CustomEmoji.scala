package models

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, OFormat}

implicit val config: JsonConfiguration.Aux[Json.MacroOptions] =
  JsonConfiguration(SnakeCase)

case class CustomEmoji(
    shortcode: String,
    url: String,
    staticUrl: String,
    visibleInPicker: Boolean,
    category: Option[String] = None
)

object CustomEmoji {
  implicit val customEmojiFormat: OFormat[CustomEmoji] =
    Json.format[CustomEmoji]
}
