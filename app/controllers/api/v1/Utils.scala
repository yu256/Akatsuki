package controllers.api.v1

import play.api.libs.json.*
import play.api.mvc.*

object Utils {
  def toJsonResponse[A](serializable: A): Writes[A] ?=> Result =
    Results.Ok(Json.toJson(serializable))
}
