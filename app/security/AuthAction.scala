package security

import cats.syntax.all.*
import play.api.mvc.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userId: Long, request: Request[A])
    extends WrappedRequest[A](request)

@Singleton
class AuthAction @Inject() (
    authRepo: repositories.AuthRepository,
    cc: ControllerComponents
)(using ec: ExecutionContext)
    extends AbstractController(cc) {
  private val actionFunction = new ActionFunction[Request, UserRequest] {
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
          case None        => Unauthorized.pure
        }
  }

  def apply[A](
      bodyParser: BodyParser[A] = parse.anyContent
  ): ActionBuilder[UserRequest, A] =
    actionFunction compose Action(bodyParser)
}
