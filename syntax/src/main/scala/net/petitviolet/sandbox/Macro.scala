package net.petitviolet.sandbox

object Macro {
  import scala.quoted.*
  inline def timeLogging[T](inline f: => T): T = {
    ${ timeLoggingImpl[T]('f) }
  }

  private def timeLoggingImpl[T: Type](f: Expr[T])(using q: Quotes): Expr[T] =
    '{
      val start = System.nanoTime()
      println(s"[start]$start")
      val r = $f
      val end = System.nanoTime()
      println(s"[ end ]$end - elapsed: ${(end - start) / 1000000}ms")
      r
    }
}
