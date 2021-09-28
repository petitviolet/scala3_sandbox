package net.petitviolet.sandbox
import scala.annotation.targetName

object Syntax:

  class MyValue(value: Int) {
    def isOdd: Boolean = value % 2 != 0
  }

  trait Adder[T]:
    def add(a: T, b: T): T

  opaque type AnotherNumber = Int
  object AnotherNumber:
    def apply(i: Int): AnotherNumber = i

  /**
   * Added @targetName annotation to suppress the following error message
   * [error] 23 |    def value: Int = n
   * [error]    |        ^
   * [error]    |Double definition:
   * [error]    |def value(n: net.petitviolet.sandbox.Syntax.AnotherNumber): Int in object Syntax at line 17 and
   * [error]    |def value(n: net.petitviolet.sandbox.Syntax.Number): Int in object Syntax at line 23
   * [error]    |have the same type after erasure.
   * [error]    |
   * [error]    |Consider adding a @targetName annotation to one of the conflicting definitions
   * [error]    |for disambiguation.
   */
  extension (n: AnotherNumber)
    @targetName("n_AnotherNumber") def value: Int = n

  opaque type Number = Int
  object Number:
    def apply(i: Int): Number = i
  extension (n: Number)
    @targetName("n_Number") def value: Int = n

  // inject a method to all of A instances only if A has an Adder[A] instance
  extension [A](a: A)
    // contextual function
    def add(b: A): Adder[A] ?=> A = {
      // implicitly[T]
      summon[Adder[A]].add(a, b)
    }

  given Adder[Number] = new Adder[Number]:
    override def add(a: Number, b: Number): Number = a + b

  opaque type UserId = Long
  // to instantiate an opaque type it seems to need constructor
  object UserId:
    def apply(id: Long): UserId = id

  opaque type UserEmail = String
  object UserEmail:
    def apply(email: String): UserEmail = email

  // union type
  type UserIdentifier = UserId | UserEmail
  case class User(id: UserId, email: UserEmail)

  // [error] .../Syntax.scala:37: error: ] expected but identifier found
  // [error]   given Conversion[Long | String, UserIdentifier] with
  // given Conversion[Long | String, UserIdentifier] with
  type IdLike = Long | String
  // implicit type conversion
  given Conversion[IdLike, UserIdentifier] with
    def apply(idLike: Long | String): UserIdentifier = idLike match {
      case id: Long      => UserId(id)
      case email: String => UserEmail(email)
    }

  object User:
    val users: Seq[User] =
      User(1, "alice@example.com") :: User(2, "bob@example.com") :: Nil

    def find(identifier: UserIdentifier): Option[User] =
      val f: User => Boolean = identifier match
        case id: UserId       => { _.id == id }
        case email: UserEmail => { _.email == email }
      users.find(f)
  end User

  trait HasWidth:
    def width: Double

  trait HasHeight:
    def height: Double

  // intersection type
  def area(x: HasWidth & HasHeight) = x.width * x.height

  object DependentType:
    trait Value {
      type Input
      type Output
      def input: Input
    }
    object Value {
      // scalafmt `raises Search state exploded on` error...
      type Type = [I] =>> [O] =>> Value { type Input = I; type Output = O }
      def of[I, O](i: I): Type[I][O] = new Value {
        type Input = I
        type Output = O
        val input = i
      }
    }

    trait Processor[V <: Value] {
      val value: V
      def process(input: value.Input): value.Output
      def processValue(v: V): v.Output
    }

end Syntax // only `end` works

enum Currency(val quantity: Double) {
  case Yen(q: Double) extends Currency(q)
  case Dollar(q: Double) extends Currency(q)

  def yen: Currency = this match {
    case y: Yen    => y
    case Dollar(d) => Yen(d * 110.69)
  }
  def dollar: Currency = this match {
    case Yen(q)    => Dollar(q / 110.69)
    case d: Dollar => d
  }
}

val currencyAdder: Syntax.Adder[Currency] = new Syntax.Adder[Currency]:
  override def add(a: Currency, b: Currency): Currency = (a, b) match {
    case (Currency.Yen(a), Currency.Yen(b)) => Currency.Yen(a + b)
    case (Currency.Yen(a), b: Currency.Dollar) =>
      Currency.Yen(a + b.yen.quantity)
    case (Currency.Dollar(a), b: Currency.Yen) =>
      Currency.Yen(a + b.dollar.quantity)
    case (Currency.Dollar(a), Currency.Dollar(b)) => Currency.Dollar(a + b)
  }

@main def main: Unit = {
  {
    val v = Syntax.MyValue(100) // `new` not required
    println(if v.isOdd then "odd" else "even") // `then` w/o `()`
  }

  {
    import Syntax.{Number, given} // import given values
    val n: Number = Number(100)
    println(n.add(Number(200)))
  }

  {
    import Syntax._
    case class Rectangle(width: Double, height: Double)
        extends HasHeight,
          HasWidth
    println(area(Rectangle(10, 20)))
  }

  {
    import Syntax.{User, UserEmail, UserId}
    println(User.find(UserId(1)))
    println(User.find(UserEmail("bob@example.com")))

    import Syntax.given_Conversion_IdLike_UserIdentifier
    import scala.language.implicitConversions
    println(User.find(2L))
  }

  {
    import Syntax.{add, Adder}
    val c: Currency = Currency.Yen(100)
    given Adder[Currency] = currencyAdder
  }

  {
    import Syntax.DependentType._
    val intValue: Value.Type[Int][String] = Value.of[Int, String](100)
    val intProcessor = new Processor[Value.Type[Int][String]] {
      override val value = intValue
      override def process(input: value.Input): value.Output = s"processed: ${input}"
      override def processValue(v: Value.Type[Int][String]): v.Output = s"processedValue: ${v.input}"
    }
    println(intProcessor.process(intValue.input))
    println(intProcessor.processValue(intValue))
  }

  {
    Macro.timeLogging {
      println("hello")
      Thread.sleep(100)
      println("world!")
    }
  }

  {
    case class Type(name: String, field: Seq[Type])
    given TypeA: Type = Type("A", TypeB :: Nil)
    given TypeB: Type = Type("B", TypeA :: Nil)

    // given values are evaluated when they are required
    // println(s"TypeA: ${TypeA}") // cause StackOverflowError

    given TypeC: Type = {
      // only once
      println(s"TypeC is initiated")
      Type("C", Nil)
    }
    println(s"TypeC 1: ${TypeC}")
    println(s"TypeC 2: ${TypeC}")
    println(s"TypeC 3: ${TypeC}")
  }
}
