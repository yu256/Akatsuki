package controllers.api.v1

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.mvc.*
import repositories.AuthRepository
import security.AuthController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FiltersController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider
)(using
    ExecutionContext
) extends AuthController(authRepo, cc, dbConfigProvider) {
  val get: Action[AnyContent] = authAction() {
    Function.const(Ok(Json.arr())) // todo
  }
}
