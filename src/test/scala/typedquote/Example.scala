package typedquote

object Example extends ShowAdapterOps {

  case class User(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val user = User("Paolo", 42)
    val encodedString = log"My first typesafe log: [$user]" //1
    println(encodedString)
  }

  implicit val userEncoder: MyShow[User] = new MyShow[User] {

    def print(value: User): String = {
      s"User(name = ${value.name}, age = ${value.age})"
    }

  }

}

trait MyShow[A] {

  def print(value: A): String

}

trait ShowAdapterOps {

  @inline
  implicit def toShowAdapter(sc: StringContext): ShowAdapter = new ShowAdapter(sc)

}

final class ShowAdapter(val sc: StringContext) extends AnyVal {

  def log(args: Any*): String = macro InterpolationMacro.gen //1

}

object ShowAdapter {

  type Typeclass[A] = MyShow[A] //2

  def adapt[A](value: A, typeclass: Typeclass[A]): String = { //3
    typeclass.print(value)
  }

}