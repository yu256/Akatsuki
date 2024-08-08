package TestUtils

import filters.ErrorHandlingFilter
import org.apache.pekko.stream.Materializer
import play.api.http.Writeable
import play.api.mvc.{EssentialAction, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

def callWithFilter[A](action: EssentialAction, req: Request[A])(using
    w: Writeable[A]
): (Materializer, ExecutionContext) ?=> Future[Result] = {
  import org.apache.pekko.stream.scaladsl.Source
  import play.api.http.HeaderNames.*
  val bytes = w.transform(req.body)

  val contentType =
    req.headers.get(CONTENT_TYPE).orElse(w.contentType).map(CONTENT_TYPE -> _)
  val contentLength = req.headers
    .get(CONTENT_LENGTH)
    .orElse(Some(bytes.length.toString))
    .map(CONTENT_LENGTH -> _)
  val newHeaders =
    req.headers.replace(contentLength.toSeq ++ contentType.toSeq*)
  new ErrorHandlingFilter()
    .apply(action)(req.withHeaders(newHeaders))
    .run(Source.single(bytes))
}
