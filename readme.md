# TypedQuote
[![Download](https://api.bintray.com/packages/irevive/maven/typedquote/images/download.svg)](https://bintray.com/irevive/maven/typedquote/_latestVersion)

TypedQuote provides a type-safe string interpolation.
 
## Summary
Main idea behind this library to provide a type-safe way to log entities.  
TypedQuote is a thin library that will provide a way to make type-safe logging on compile time.  
Library supports any typeclass cats.Show, scalaz.Show and even your own typeclass.  
  
Inspired by [Magnolia](https://github.com/propensive/magnolia).

## Installation:

```
resolvers += Resolver.bintrayRepo("irevive", "maven")

libraryDependencies += "io.github.irevive" %% "typedquote" % version

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

## Typeclass adapter
First of all, TypedQuote requires an adapter that will encode entries with a specified typeclass.  
  
A basic object will follow this structure:  

```scala
trait MyShow[A] {

    def print(value: A): String
    
}

trait ShowAdapterOps {
  @inline
  implicit def toShowAdapter(sc: StringContext): ShowAdapter = new ShowAdapter(sc)
}

final class ShowAdapter(val sc: StringContext) extends AnyVal {

  def log(args: Any*): String = macro typedquote.InterpolationMacro.gen //1

}

object ShowAdapter {

  type Typeclass[A] = MyShow[A] //2

  def adapt[A](value: A, typeclass: Typeclass[A]): String = { //3
    typeclass.print(value)
  }

}
```

More details about the structure:  
1) Method `log` is an enrichment for StringContext, the method is used for the string interpolation. `InterpolationMacro.gen` will generate encoding of arguments with a typeclass;    
2) The `MyShow` typeclass will be used for encoding: cats.Show, scalaz.Show or your own typeclass;  
3) The adapter method that will encode entity with a provided typeclass.       


## Type-safe logging
According to example from above, StringContext has an enrichment method `log`.  
The `log` prefix will generate a type-safe string using `MyShow` instance for each argument of the string interpolation.  

Example:  
```scala
object Main extends ShowAdapterOps {
  
  case class User(name: String, age: Int)
  
  def main(args: Array[String]): Unit = {
    val user = User("Paolo", 42)  
    val encodedString = log"My first typesafe log: [$user]" //1
    println(encodedString)
  }

}
```

This code (1) will be transformed to:  
```scala
val encodedString = {
  StringContext("My first typesafe log. User: [", "]").s(ShowAdapter.adapt(user, implicitly[ShowAdapter.Typeclass[User]]))
}
```

Current example will not compile, because there is no available `MyShow` typeclass for the type `User`.  
Define an encoder for the type `User`: 
```scala
implicit val userEncoder: MyShow[User] = new MyShow[User] {
  
  def print(value: User): String = {
    s"User(name = ${value.name}, age = ${value.age})"
  }

}
```

Currently, example will compile. And the result of encoded string will be:
```
My first typesafe log. User: [User(name = Paolo, age = 42)]
```

Full example you can find [here](https://github.com/iRevive/typedquote/tree/master/src/test/scala/typedquote/Example.scala).

## Debug
Add parameter `-Dtypedquote.debug=true` to the compilation process to see the output. 