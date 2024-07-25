package repositories

import cats.data.{EitherT, OptionT}
import extensions.DBIOA

import scala.concurrent.Future

private trait Repository {
  def run[T]: DBIOA[T] => Future[T]
  def runM[T](value: DBIOA[Option[T]]): OptionT[Future, T] = OptionT(
    run(value)
  )
  def runM[A, B](value: DBIOA[Either[A, B]]): EitherT[Future, A, B] =
    EitherT(run(value))
}
