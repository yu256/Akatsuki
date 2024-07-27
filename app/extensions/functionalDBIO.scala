package extensions

import cats.{
  Applicative,
  Apply,
  Functor,
  Monad,
  MonadError,
  Monoid,
  Semigroupal
}
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}

import scala.concurrent.ExecutionContext

object functionalDBIO {
  extension [R, S <: NoStream](
      fa: DBIOAction[R, S, Effect.All]
  ) {
    def asEither: ExecutionContext ?=> DBIO[Either[Throwable, R]] =
      fa.asTry.map(_.toEither)
  }
  given (using ExecutionContext): Functor[DBIO] with
    override def map[A, B](fa: DBIO[A])(
        f: A => B
    ): DBIO[B] = fa.map(f)

  given (using ExecutionContext): Monad[DBIO] with
    override def pure[A](x: A): DBIO[A] = DBIO.successful(x)
    override def flatMap[A, B](fa: DBIO[A])(
        f: A => DBIO[B]
    ): DBIO[B] = fa.flatMap(f)
    // not stack safe
    override def tailRecM[A, B](a: A)(
        f: A => DBIO[Either[A, B]]
    ): DBIO[B] =
      f(a).flatMap {
        case Left(nextA) => tailRecM(nextA)(f)
        case Right(b)    => DBIO.successful(b)
      }

  given [E <: Throwable](using ExecutionContext): MonadError[DBIO, E] with
    override def raiseError[A](e: E): DBIO[A] = DBIO.failed(e)
    override def handleErrorWith[A](fa: DBIO[A])(
        f: E => DBIO[A]
    ): DBIO[A] = fa.asTry.flatMap {
      case scala.util.Success(a) => DBIO.successful(a)
      case scala.util.Failure(e) => f(e.asInstanceOf[E])
    }
    override def pure[A](x: A): DBIO[A] = DBIO.successful(x)
    override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] =
      fa.flatMap(f)
    override def tailRecM[A, B](a: A)(f: A => DBIO[Either[A, B]]): DBIO[B] =
      f(a).flatMap {
        case Left(nextA) => tailRecM(nextA)(f)
        case Right(b)    => DBIO.successful(b)
      }

  given (using ExecutionContext): Semigroupal[DBIO] with
    override def product[A, B](fa: DBIO[A], fb: DBIO[B]): DBIO[(A, B)] =
      fa.zip(fb)

  given [A](using ExecutionContext, Monoid[A]): Monoid[DBIO[A]] with
    override def empty: DBIO[A] = DBIO.successful(Monoid[A].empty)
    override def combine(x: DBIO[A], y: DBIO[A]): DBIO[A] =
      x.zipWith(y)(Monoid[A].combine)

  given (using ExecutionContext): Applicative[DBIO] with
    override def pure[A](x: A): DBIO[A] = DBIO.successful(x)
    override def ap[A, B](ff: DBIO[A => B])(fa: DBIO[A]): DBIO[B] =
      ff.flatMap(fa.map)

  given (using ExecutionContext): Apply[DBIO] with
    override def ap[A, B](ff: DBIO[A => B])(fa: DBIO[A]): DBIO[B] =
      ff.flatMap(fa.map)
    override def map[A, B](fa: DBIO[A])(f: A => B): DBIO[B] =
      fa.map(f)
    override def map2[A, B, Z](
        fa: DBIO[A],
        fb: DBIO[B]
    )(f: (A, B) => Z): DBIO[Z] =
      fa.zipWith(fb)(f)

}
