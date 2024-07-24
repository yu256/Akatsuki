package models

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, OFormat}

case class CustomEmoji(
    shortcode: String,
    url: String,
    staticUrl: String,
    visibleInPicker: Boolean,
    category: Option[String] = None
)

object CustomEmoji {
  given JsonConfiguration.Aux[Json.MacroOptions] =
    JsonConfiguration(SnakeCase)
  given OFormat[CustomEmoji] =
    Json.format[CustomEmoji]
}
