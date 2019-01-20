package berlin.jentsch.modelchecker.benchmarks

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(time = 1, iterations = 3)
@Warmup(time = 1, iterations = 2)
class ReferenceBenchmarks {

  private var x = 0

  @Benchmark
  def plainInt: Int = {
    val r = x
    x += 1
    r
  }

  private val atomic = new AtomicInteger()

  @Benchmark
  def atomicInt: Int =
    atomic.getAndIncrement()
}
