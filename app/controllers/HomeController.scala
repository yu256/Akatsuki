package controllers
import controllers.api.v1.AccountsController.userForm
import play.api.i18n.I18nSupport
import play.api.mvc.*
import repositories.{AccountRepository, AuthRepository, UserRepository}

import javax.inject.*
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject() (
    cc: ControllerComponents,
    userRepo: UserRepository,
    accountRepo: AccountRepository,
    authRepo: AuthRepository
)(using
    ExecutionContext
) extends AbstractController(cc)
    with I18nSupport {
  def index(redirect: Option[String] = None): Action[AnyContent] = Action {
    implicit request =>
      Ok(views.html.index(userForm, redirect))
  }
}
