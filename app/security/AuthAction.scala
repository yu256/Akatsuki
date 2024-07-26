package security

import cats.syntax.all.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.*
import repositories.AuthRepository
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userId: Long, request: Request[A])
    extends WrappedRequest[A](request)

private class AuthActionFunction(authRepo: AuthRepository)(using
    ec: ExecutionContext
) extends ActionFunction[Request, UserRequest] {
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](
      request: Request[A],
      block: UserRequest[A] => Future[Result]
  ): Future[Result] =
    request.headers
      .get("Authorization")
      .withFilter(_.startsWith("Bearer "))
      .map(_.drop(7))
      .flatTraverse(authRepo.findToken andThen authRepo.run)
      .flatMap {
        case Some(token) => block(UserRequest(token.resourceOwnerId, request))
        case None        => Results.Unauthorized.pure
      }
}

@Singleton
class AuthAction @Inject() (
    authRepo: repositories.AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider
)(using ec: ExecutionContext)
    extends AbstractController(cc) {
  private val actionFunction = AuthActionFunction(authRepo)
  private val dbConfig = dbConfigProvider.get[PostgresProfile]

  def apply[A](
      bodyParser: BodyParser[A] = parse.anyContent
  ): ActionBuilder[UserRequest, A] =
    actionFunction compose Action(bodyParser)

  def asyncDB[A](bodyParser: BodyParser[A] = parse.anyContent)(
      block: UserRequest[A] => DBIO[Result]
  ): Action[A] =
    apply[A](bodyParser).async(block andThen dbConfig.db.run)
}
