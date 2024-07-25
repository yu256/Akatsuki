package extensions

import cats.{Apply, Functor, Monad, Monoid, Semigroupal}
import slick.dbio.{DBIO, DBIOAction, NoStream}

import scala.concurrent.ExecutionContext

type DBIOA[T] = DBIOAction[T, NoStream, Nothing]

class Functional(using ExecutionContext) {
  implicit lazy val functorForDBIOAction: Functor[DBIOA] =
    new Functor[DBIOA] {
      override def map[A, B](fa: DBIOA[A])(
          f: A => B
      ): DBIOA[B] = fa.map(f)
    }
  implicit lazy val monadForDBIOAction: Monad[DBIOA] =
    new Monad[DBIOA] {
      override def pure[A](x: A): DBIOA[A] = DBIO.successful(x)
      override def flatMap[A, B](fa: DBIOA[A])(
          f: A => DBIOA[B]
      ): DBIOA[B] = fa.flatMap(f)
      // not stack safe
      override def tailRecM[A, B](a: A)(
          f: A => DBIOA[Either[A, B]]
      ): DBIOA[B] =
        f(a).flatMap {
          case Left(nextA) => tailRecM(nextA)(f)
          case Right(b)    => DBIO.successful(b)
        }
    }
  implicit lazy val dbioForSemigroupal: Semigroupal[DBIOA] =
    new Semigroupal[DBIOA] {
      override def product[A, B](fa: DBIOA[A], fb: DBIOA[B]): DBIOA[(A, B)] =
        fa.zip(fb)
    }
  implicit def dbioForMonoid[A](implicit a: Monoid[A]): Monoid[DBIOA[A]] =
    new Monoid[DBIOA[A]] {
      override def empty: DBIOA[A] = DBIO.successful(a.empty)
      override def combine(x: DBIOA[A], y: DBIOA[A]): DBIOA[A] =
        x.zipWith(y)(a.combine)
    }

  implicit lazy val applicativeForDBIOAction: cats.Applicative[DBIOA] =
    new cats.Applicative[DBIOA] {
      override def pure[A](x: A): DBIOA[A] = DBIO.successful(x)
      override def ap[A, B](ff: DBIOA[A => B])(fa: DBIOA[A]): DBIOA[B] =
        ff.flatMap(f => fa.map(f))
    }

  implicit lazy val applyForDBIOAction: Apply[DBIOA] =
    new Apply[DBIOA] {
      override def ap[A, B](ff: DBIOA[A => B])(fa: DBIOA[A]): DBIOA[B] =
        ff.flatMap(f => fa.map(f))
      override def map[A, B](fa: DBIOA[A])(f: A => B): DBIOA[B] =
        fa.map(f)
      override def map2[A, B, Z](
          fa: DBIOA[A],
          fb: DBIOA[B]
      )(f: (A, B) => Z): DBIOA[Z] =
        fa.zipWith(fb)(f)
    }
}
