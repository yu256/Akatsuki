package extensions

import scala.annotation.targetName

object ChainingOps {
  // equals to scala.util.ChainingOps
  extension [A](v: A) {
    @targetName("pipeline")
    infix def |>[B](f: A => B): B = f(v)

    @targetName("tap")
    infix def ?>[B](f: A => ?): A = { f(v); v }
  }
}
