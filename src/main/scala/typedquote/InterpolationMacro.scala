package typedquote

import scala.reflect.macros._

object InterpolationMacro {

  def gen(c: whitebox.Context)(args: c.Expr[Any]*): c.Expr[String] = {
    import c.universe._

    val debugEnabled = sys.props.get("typedquote.debug").isDefined

    val prefixType = c.prefix.tree.tpe
    val prefixObject = prefixType.typeSymbol
    val companion = prefixType.companion

    def error(msg: String) = c.abort(c.enclosingPosition, msg)

    val extractorClass: c.universe.Type = if (prefixObject.isClass) {
      if (companion != NoType) {
        companion
      } else {
        error(s"typedquote: the $prefixObject must have a companion class with the defined type `Typeclass` and the `adapt` method")
      }
    } else {
      prefixType
    }

    val typeDefs = extractorClass.baseClasses.flatMap { cls =>
      cls.asType.toType.decls.filter(_.isType).find(_.name.toString == "Typeclass").map { tpe =>
        tpe.asType.toType.asSeenFrom(extractorClass, cls)
      }
    }

    if (typeDefs.headOption.isEmpty) {
      error(s"typedquote: the derivation $prefixObject does not define the Typeclass type constructor")
    }

    def select(identifier : String) : Tree = {
      val terms = identifier.split("\\.").map { TermName(_) }
      terms.tail.foldLeft[Tree](Ident(terms.head)) { Select(_, _) }
    }

    def extractMethod() = {
      val term = TermName("adapt")

      val combineClass = extractorClass.baseClasses
        .find(cls => cls.asType.toType.decl(term) != NoSymbol)
        .getOrElse(error(s"typedquote: the method `def adapt(value: A, tc: Typeclass[A]): String` must be defined in the $prefixObject"))

      val method = combineClass.asType.toType.decl(term).asTerm.asMethod

      val firstParamBlock = method.paramLists.head

      if (firstParamBlock.lengthCompare(2) != 0) {
        error(s"typedquote: the method `adapt` should take two parameters of type A and Typeclass[A]")
      }

      method
    }

    val adaptMethod = extractMethod()

    val scParts = c.prefix.tree
      .collect {
        case Apply(Select(Select(Ident(TermName("scala")), TermName("StringContext")), TermName("apply")), parts) => parts
      }
      .headOption
      .toList
      .flatten

    val convertedArguments = args map { arg =>
      val adaptMethodSelect = select(adaptMethod.fullName)
      val typeclassSelect = Select(select(prefixObject.fullName), TypeName("Typeclass"))

      q"$adaptMethodSelect(${arg.tree}, implicitly[$typeclassSelect[${arg.actualType}]])"
    }

    val result = q"StringContext(..$scParts).s(..$convertedArguments)"

    if (debugEnabled) {
      println(result)
    }

    c.Expr[String](result)
  }

}
