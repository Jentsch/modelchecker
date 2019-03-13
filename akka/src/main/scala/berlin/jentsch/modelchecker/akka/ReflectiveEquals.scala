package berlin.jentsch.modelchecker.akka

import scala.collection.concurrent.TrieMap
import scala.reflect.runtime.universe.runtimeMirror
import scala.tools.reflect.{ToolBox, ToolBoxError}

/**
  * Determine equality of to object with runtime reflection.
  *
  * @example Some examples
  * {{{
  *   import org.scalatest.prop.Tables._
  *
  *   def examples = Table(
  *     ("a", "b", "expected"),
  *     ("x", "x", true),
  *     ("x", "y", false),
  *     (str, str, true),
  *     (str("a"), str("a"), true),
  *     (str("a"), str("b"), false),
  *     (str("a")("b"), str("a")("b"), true),
  *     (int(1), int(1), true),
  *     (int(0), int(2), false)
  *   )
  *
  *   def str: String => String => String = x => y => x ++ y
  *   def int: Int => Int => Int = x => y => x + y
  *
  *   examples.forEvery{ (a: AnyRef, b: AnyRef, expected: Boolean) =>
  *     ReflectiveEquals(a, b) should be(expected)
  *   }
  *
  * }}}
  */
object ReflectiveEquals {
  def apply(a: AnyRef, b: AnyRef): Boolean =
    equals(a, b)

  private val generatedEquals = TrieMap.empty[Class[_], ReflectiveEquals]

  private val tb = runtimeMirror(getClass.getClassLoader).mkToolBox()

  def equals(a: AnyRef, b: AnyRef): Boolean =
    if (a eq b) {
      true
    } else if (a.isInstanceOf[Any => Any] || a.isInstanceOf[() => Any] || a
                 .isInstanceOf[(Any, Any) => Any]) {
      generatedEquals
        .getOrElseUpdate(
          a.getClass, {
            val fields = a.getClass.getDeclaredFields

            val code = s""" (clazz: Class[_]) => {
                | import berlin.jentsch.modelchecker.akka.ReflectiveEquals
                |
                | new ReflectiveEquals {
                |   private val fields = clazz.getDeclaredFields
                |   fields.foreach(_.setAccessible(true))
                |   val Array(${fields.indices
                            .map("f" + _)
                            .mkString(", ")}) = fields
                |
                |   override def compare(a: AnyRef, b: AnyRef): Boolean = {
                |     if (clazz == b.getClass) {
                |
                |       ${fields.zipWithIndex
                            .map {
                              case (f, i) =>
                                if (f.getType == classOf[Int]) {
                                  s"if (f$i.getInt(a) != f$i.getInt(b)) return false"
                                } else {
                                  s"if (! ReflectiveEquals(f$i.get(a), f$i.get(b))) return false"
                                }
                            }
                            .mkString("\n")}
                |
                |       return true
                |     } else false
                |   }
                | }
                | }
              """.stripMargin

            val tree = try { tb.parse(code) } catch {
              case t: ToolBoxError =>
                println("Code was:"); println(code); throw t
            }

            tb.compile(tree)()
              .asInstanceOf[Class[_] => ReflectiveEquals](a.getClass)
          }
        )
        .compare(a, b)
    } else a == b
}

trait ReflectiveEquals {
  def compare(a: AnyRef, b: AnyRef): Boolean
}
