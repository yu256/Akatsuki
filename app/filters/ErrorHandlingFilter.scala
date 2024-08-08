package filters

import org.apache.pekko.stream.Materializer
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.*

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandlingFilter @Inject() (implicit
    val mat: Materializer,
    ec: ExecutionContext
) extends Filter
    with Logging {
  override def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).recover { ex =>
      logger.error("An error occurred", ex)
      Results.InternalServerError(
        Json.obj("error" -> "Internal server error occurred")
      )
    }
  }
}
