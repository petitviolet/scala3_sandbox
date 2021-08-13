package net.petitviolet.sandbox

object Syntax:
  trait Adder[T]:
    def add(a: T, b: T): T

  opaque type Number = Int
  object Number:
    def apply(i: Int): Number = i

  extension [A](a: A)
    def add(b: A): Adder[A] ?=> A = {
      // implicitly[T]
      summon[Adder[A]].add(a, b)
    }

  given Adder[Number] = new Adder[Number]:
    override def add(a: Number, b: Number): Number = a + b

end Syntax // only `end` works

enum Currency(val quantity: Double) {
  case Yen(q: Double) extends Currency(q)
  case Dollar(q: Double) extends Currency(q)

  def yen: Currency = this match {
    case y: Yen => y
    case Dollar(d) => Yen(d * 110.69)
  }
  def dollar: Currency = this match {
    case Yen(q) => Dollar(q / 110.69)
    case d: Dollar => d
  }
}

val currencyAdder: Syntax.Adder[Currency] = new Syntax.Adder[Currency]:
  override def add(a: Currency, b: Currency): Currency = (a, b) match {
    case (Currency.Yen(a), Currency.Yen(b)) => Currency.Yen(a + b)
    case (Currency.Yen(a), b: Currency.Dollar) => Currency.Yen(a + b.yen.quantity)
    case (Currency.Dollar(a), b: Currency.Yen) => Currency.Yen(a + b.dollar.quantity)
    case (Currency.Dollar(a), Currency.Dollar(b)) => Currency.Dollar(a + b)
  }

@main def main: Unit =
  import Syntax._
  {
    val n: Number = Number(100)
    println(n.add(Number(200)))
  }

  {
    val c: Currency = Currency.Yen(100)
    given Adder[Currency] = currencyAdder
    println(c.add(Currency.Dollar(200)))
  }

