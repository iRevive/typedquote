package typedquote

import org.scalatest.{Matchers, WordSpec}

class InterpolationMacroSpec extends WordSpec with Matchers {

  implicit def toShowAdapter(sc: StringContext): TestShowAdapter = new TestShowAdapter(sc)

  "InterpolationMacro" when {

    "missing typeclass for an argument" should {

      "fail to compile" in {

          """
            |val stringArg = "s"
            |val intArg = 123
            |val longArg = 321L
            |log"log $stringArg $intArg $longArg"
            |
        """.stripMargin shouldNot compile
      }

    }

    "has all arguments" should {

      "compile" in {

        """
          |val stringArg = "s"
          |val intArg = 123
          |log"log $stringArg $intArg"
          |
        """.stripMargin should compile
      }

    }
  }

}

trait TestShow[A] {

  def show(value: A): String

}

object TestShow {

  implicit val intShow: TestShow[Int] = new TestShow[Int] {
    override def show(value: Int): String = value.toString
  }

  implicit val stringShow: TestShow[String] = new TestShow[String] {
    override def show(value: String): String = value
  }

}

final class TestShowAdapter(sc: StringContext) {

  def log(args: Any*): String = macro InterpolationMacro.gen

}

object TestShowAdapter {

  type Typeclass[A] = TestShow[A]

  def adapt[A](value: A, typeclass: Typeclass[A]): String = {
    typeclass.show(value)
  }

}