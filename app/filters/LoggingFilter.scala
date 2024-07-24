package filters

import org.apache.pekko.stream.Materializer
import play.api.Logging
import play.api.mvc.*

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoggingFilter @Inject() (implicit
    val mat: Materializer,
    ec: ExecutionContext
) extends Filter
    with Logging {
  override def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] =
    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      println(
        s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}"
      )

      result
    }
}
