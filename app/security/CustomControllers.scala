package security

import cats.data.{EitherT, OptionT}
import cats.syntax.all.*
import extensions.PolyFunc.~>
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.*
import repositories.{AuthRepository, Tables}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](user: Tables.UsersRow, request: Request[A])
    extends WrappedRequest[A](request)

case class OptionalUserRequest[A](
    user: Option[Tables.UsersRow],
    request: Request[A]
) extends WrappedRequest[A](request)

private abstract class AuthActionFunctionBase[R[_]](
    authRepo: AuthRepository,
    runner: DBIO ~> Future
)(using ec: ExecutionContext)
    extends ActionFunction[Request, R] {
  override def executionContext: ExecutionContext = ec
  protected def partialInvokeBlock[A](
      request: Request[A],
      block: R[A] => Future[Result]
  ): Future[Option[repositories.Tables.UsersRow]] =
    request.headers
      .get("Authorization")
      .withFilter(_.startsWith("Bearer "))
      .map(_.drop(7))
      .flatTraverse(authRepo.findUserByToken andThen runner.apply)
}

private class AuthActionFunction(
    authRepo: AuthRepository,
    runner: DBIO ~> Future
)(using ExecutionContext)
    extends AuthActionFunctionBase[UserRequest](authRepo, runner) {
  override def invokeBlock[A](
      request: Request[A],
      block: UserRequest[A] => Future[Result]
  ): Future[Result] =
    partialInvokeBlock(request, block).flatMap {
      case Some(user) => block(UserRequest(user, request))
      case None       => Results.Unauthorized.pure
    }
}

private class OptionalAuthActionFunction(
    authRepo: AuthRepository,
    runner: DBIO ~> Future
)(using ExecutionContext)
    extends AuthActionFunctionBase[OptionalUserRequest](authRepo, runner) {
  override def invokeBlock[A](
      request: Request[A],
      block: OptionalUserRequest[A] => Future[Result]
  ): Future[Result] =
    partialInvokeBlock(request, block).flatMap { userOpt =>
      block(OptionalUserRequest(userOpt, request))
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
  private val runner: DBIO ~> Future = [A] => (fa: DBIO[A]) => run(fa)
  private val actionFunction =
    AuthActionFunction(authRepo, runner)
  private val optionalAuthFunction =
    OptionalAuthActionFunction(authRepo, runner)

  def authAction[A](
      bodyParser: BodyParser[A] = parse.anyContent
  ): ActionBuilder[UserRequest, A] =
    actionFunction compose Action(bodyParser)

  def optionalAuthAction[A](
      bodyParser: BodyParser[A] = parse.anyContent
  ): ActionBuilder[OptionalUserRequest, A] =
    optionalAuthFunction compose Action(bodyParser)

  def authActionDB[A](bodyParser: BodyParser[A] = parse.anyContent)(
      block: UserRequest[A] => DBIO[Result]
  ): Action[A] =
    authAction[A](bodyParser).async(block andThen run)

  def optionalAuthActionDB[A](bodyParser: BodyParser[A] = parse.anyContent)(
      block: OptionalUserRequest[A] => DBIO[Result]
  ): Action[A] =
    optionalAuthAction[A](bodyParser).async(block andThen run)
}
