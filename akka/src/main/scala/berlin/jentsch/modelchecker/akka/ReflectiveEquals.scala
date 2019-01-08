package berlin.jentsch.modelchecker.akka

/**
  * @example
  * {{{
  *   cases.foreach{ case (a, b, expected: Boolean) =>
  *     ReflectiveEquals(a, b) should be(expected)
  *   }
  *
  *   def cases: List[(Any, Any, Boolean)] = List(
  *     ("x", "x", true),
  *     ("x", "y", false),
  *     (x, x, true),
  *     (x(1), x(1), true),
  *     (x(1), x(2), false),
  *     (x(1)(2), x(2)(1), true)
  *   )
  *
  *   def x: Int => Int => Int = x => y => x + y
  * }}}
  */
object ReflectiveEquals {
  def apply(a: Any, b: Any): Boolean = equals(a, b)

  def equals(a: Any, b: Any): Boolean =
    if (classOf[Function[Any, Any]].isInstance(a)) {
      val ac = a.getClass
      if (ac == b.getClass) {

        val fields = ac.getDeclaredFields

        var i = 0
        while (i < fields.length) {
          val f = fields(i)
          if (!f.isAccessible)
            f.setAccessible(true)

          if (! equals(f.get(a), f.get(b)))
            return false

          i += 1
        }

        return true
      } else false
    } else a == b
}
