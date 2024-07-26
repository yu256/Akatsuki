package repositories

import cats.data.{EitherT, OptionT}
import slick.dbio.DBIO

import scala.concurrent.Future

private trait Repository {
  def run[T]: DBIO[T] => Future[T]
  def runM[T](value: DBIO[Option[T]]): OptionT[Future, T] = OptionT(
    run(value)
  )
  def runM[A, B](value: DBIO[Either[A, B]]): EitherT[Future, A, B] =
    EitherT(run(value))
}
