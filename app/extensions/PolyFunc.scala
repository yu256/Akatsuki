package extensions

import cats.arrow.FunctionK

import scala.annotation.targetName

object PolyFunc {
  @targetName("PolyFunc")
  type ~>[F[_], G[_]] = [A] => F[A] => G[A]

  extension [F[_], G[_]](nat: F ~> G) {
    def toFunctionK: FunctionK[F, G] =
      new FunctionK[F, G] {
        def apply[A](fa: F[A]): G[A] = nat[A](fa)
      }
  }
}
