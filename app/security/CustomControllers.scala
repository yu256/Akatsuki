package security

import cats.data.{EitherT, OptionT}
import cats.syntax.all.*
import extensions.PolyFunc.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.*
import repositories.AuthRepository
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userId: Long, request: Request[A])
    extends WrappedRequest[A](request)

private class AuthActionFunction(
    authRepo: AuthRepository,
    runner: DBIO ~> Future
)(using
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
      .flatTraverse(authRepo.findToken andThen runner.apply)
      .flatMap {
        case Some(token) => block(UserRequest(token.resourceOwnerId, request))
        case None        => Results.Unauthorized.pure
      }
}

class CustomController(
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider
) extends AbstractController(cc) {
  private val dbConfig = dbConfigProvider.get[PostgresProfile]

  def run[T] = dbConfig.db.run[T]
  def runM[T](value: DBIO[Option[T]]): OptionT[Future, T] = OptionT(
    run(value)
  )
  def runM[A, B](value: DBIO[Either[A, B]]): EitherT[Future, A, B] =
    EitherT(run(value))

  def ActionDB[A](bodyParser: BodyParser[A] = parse.anyContent)(
      block: Request[A] => DBIO[Result]
  ): Action[A] =
    Action.async(bodyParser)(block andThen run)
}

class AuthController(
    authRepo: repositories.AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider
)(using ec: ExecutionContext)
    extends CustomController(cc, dbConfigProvider) {
  private val actionFunction =
    AuthActionFunction(authRepo, [A] => (fa: DBIO[A]) => run(fa))

  def authAction[A](
      bodyParser: BodyParser[A] = parse.anyContent
  ): ActionBuilder[UserRequest, A] =
    actionFunction compose Action(bodyParser)

  def authActionDB[A](bodyParser: BodyParser[A] = parse.anyContent)(
      block: UserRequest[A] => DBIO[Result]
  ): Action[A] =
    authAction[A](bodyParser).async(block andThen run)
}
