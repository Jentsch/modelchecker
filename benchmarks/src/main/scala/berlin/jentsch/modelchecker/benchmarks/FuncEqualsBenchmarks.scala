package berlin.jentsch.modelchecker.benchmarks

import java.util.concurrent.TimeUnit

import berlin.jentsch.modelchecker.akka.ReflectiveEquals
import org.openjdk.jmh.annotations._

import scala.util.Random.{nextInt => randomInt}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(time = 1, iterations = 3)
@Warmup(time = 2, iterations = 4)
class FuncEqualsBenchmarks {

  @Benchmark
  def references: Boolean =
    RefEqual.equal(randomFunction, randomFunction)

  @Benchmark
  def caseEquals: Boolean =
    CaseEqual.equal(randomAnnotatedFunction, randomAnnotatedFunction)

  @Benchmark
  def reflexion: Boolean =
    ReflexionEqual.equal(randomFunction, randomFunction)

  @Benchmark
  def randomEquals: Boolean =
    RandomEqual.equal(randomFunction, randomFunction)

  @Benchmark
  def finalEquals: Boolean =
    ReflectiveEquals.equals(randomFunction, randomFunction)

  @Benchmark
  def serialEquals: Boolean =
    SerializableEquals.equal(randomFunction, randomFunction)

  @Param(Array("2", "4", "8"))
  var functionCount: Int = _

  @Param(Array("10"))
  var inputRange: Int = _

  private def randomFunction: Int => Int =
    allFunctions(randomInt(functionCount))(randomInt(inputRange))

  val allFunctions: Array[Int => Int => Int] = Array(
    a => b => a + b,
    a => _ => 2 * a,
    a => b => a - b,
    a => b => a max b,
    a => b => a | b,
    a => b => a ^ b,
    a => b => a & b,
    a => _ => ~a
  )

  private def randomAnnotatedFunction: Int => Int =
    allAnnotatedFunctions(randomInt(functionCount))(randomInt(inputRange))

  val allAnnotatedFunctions: Array[Int => Int => Int] = Array(
    {
      class Add(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v + v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: Add => this.v == other.v
            case _          => false
          }
      }

      new Add(_)
    }, {
      class Mul(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v * v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: Mul => this.v == other.v
            case _          => false
          }
      }

      new Mul(_)
    }, {
      class Sub(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v - v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: Sub => this.v == other.v
            case _          => false
          }
      }

      new Sub(_)
    }, {
      class Max(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v max v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: Max => this.v == other.v
            case _          => false
          }
      }

      new Max(_)
    }, {
      class Or(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v | v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: Or => this.v == other.v
            case _         => false
          }
      }

      new Or(_)
    }, {
      class XOr(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v ^ v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: XOr => this.v == other.v
            case _          => false
          }
      }

      new XOr(_)
    }, {
      class And(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = v & v1
        override def equals(obj: Any): Boolean =
          obj match {
            case other: And => this.v == other.v
            case _          => false
          }
      }

      new And(_)
    }, {
      class Not(private val v: Int) extends (Int => Int) {
        override def apply(v1: Int): Int = ~v
        override def equals(obj: Any): Boolean =
          obj match {
            case other: Not => this.v == other.v
            case _          => false
          }
      }

      new Not(_)
    }
  )

  object RefEqual {
    def equal(a1: Int => Int, a2: Int => Int): Boolean = a1.eq(a2)
  }

  object ReflexionEqual {
    val Int: Class[Int] = classOf[Int]
    val Long: Class[Long] = classOf[Long]
    val Function1: Class[Function[Any, Any]] = classOf[Function[Any, Any]]

    def equal(a1: Any, a2: Any): Boolean = {
      val xc = a1.getClass
      val yc = a2.getClass
      if (xc == yc) {
        val fields = xc.getDeclaredFields

        var i = 0
        while (i < fields.length) {
          val f = fields(i)
          if (!f.isAccessible)
            f.setAccessible(true)

          f.getType match {
            case Int =>
              if (f.getInt(a1) != f.getInt(a2))
                return false
            case Long =>
              if (f.getLong(a1) != f.getLong(a2))
                return false
            case Function1 =>
              if (!equal(f.get(a1), f.get(a2)))
                return false
            case _ =>
              if (f.get(a1) != f.get(a2))
                return false
          }

          i += 1
        }

        return true
      } else {
        return false
      }
    }
  }

  object CaseEqual {
    def equal(a1: Int => Int, a2: Int => Int): Boolean = a1 == a2
  }

  object RandomEqual {
    def equal(a1: Int => Int, a2: Int => Int): Boolean = {
      var i = 500

      while (i > 0) {
        val r = randomInt()

        if (a1(r) != a2(r))
          return false

        i -= 1
      }

      return true
    }
  }

  object SerializableEquals {
    def equal(a1: Int => Int, a2: Int => Int): Boolean = {

      def seri(a: Int => Int): Array[Byte] = {
        val buffer = new java.io.ByteArrayOutputStream()
        val obj = new java.io.ObjectOutputStream(buffer)

        obj.writeObject(a)
        obj.close()

        buffer.toByteArray
      }

      val f = seri(a1)

      val s = seri(a2)

      var i = 0

      do {
        if (f(i) != s(i))
          return false
        i += 1
      } while (i < f.length)

      return true
    }
  }
}
