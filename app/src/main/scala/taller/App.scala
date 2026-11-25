package taller

import org.scalameter._

/**
 * Punto 5: mediciones reproducibles sobre sistemas grandes. La generación y
 * la comprobación de los resultados quedan fuera de los bloques medidos.
 */
object App {

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    println(s"Núcleos disponibles: ${Runtime.getRuntime.availableProcessors}")
    println(s"Java: ${System.getProperty("java.runtime.version")}")
    println(s"Scala: ${util.Properties.versionNumberString}; ScalaMeter: 0.21")
    println(s"Heap máximo (bytes): ${Runtime.getRuntime.maxMemory}")
    println("dt = 0.5; 4 pares balanceados; Warmer.Default 5..10; benchRuns = 5; ms")

    def mediana(xs: Vector[Double]): Double = {
      val ordenados = xs.sorted
      (ordenados((xs.length - 1) / 2) + ordenados(xs.length / 2)) / 2
    }

    def muestra(cuerpo: => Sistema): Double = {
      (config(Key.exec.minWarmupRuns := 5, Key.exec.maxWarmupRuns := 10,
        Key.exec.benchRuns := 5).withWarmer(new Warmer.Default) measure {
        cuerpo.length
      }).value
    }

    val dt = 0.5
    List(500000, 1000000).foreach { n =>
      val sistema: Sistema = Array.tabulate(n) { i =>
        Particula(Vector2(i % 1000, i / 1000), Vector2((i % 7) - 3, (i % 5) - 2), 1.0 + i % 3)
      }
      val candidatos = List(n, n / 2, n / 8, n / 32)
      def comprobar(): Unit = {
        val esperado = pasoSecuencial(sistema, dt)
        candidatos.foreach(u => require(pasoParalelo(sistema, dt, u).sameElements(esperado)))
      }
      comprobar()
      candidatos.foreach { u =>
        (0 until 3).foreach { _ =>
          pasoSecuencial(sistema, dt)
          pasoParalelo(sistema, dt, u)
        }
      }
      val pares = (0 until 4).toVector.flatMap { ronda =>
        val orden = if (ronda % 2 == 0) candidatos else candidatos.reverse
        orden.map { u =>
          val (ts, tp) = if (ronda % 2 == 0) {
            val s = muestra { pasoSecuencial(sistema, dt) }
            val p = muestra { pasoParalelo(sistema, dt, u) }
            (s, p)
          } else {
            val p = muestra { pasoParalelo(sistema, dt, u) }
            val s = muestra { pasoSecuencial(sistema, dt) }
            (s, p)
          }
          println(f"muestra | $n%d | $u%d | $ronda%d | $ts%.6f | $tp%.6f")
          (u, ts, tp)
        }
      }
      println("n | umbral | tseq | tpar | S | MAD seq | MAD par | S mínimo | S máximo")
      val mediciones = candidatos.map { u =>
        val muestras = pares.filter(_._1 == u)
        val secuenciales = muestras.map(_._2)
        val paralelas = muestras.map(_._3)
        val tSecuencial = mediana(secuenciales)
        val tParalelo = mediana(paralelas)
        val madS = mediana(secuenciales.map(t => math.abs(t - tSecuencial)))
        val madP = mediana(paralelas.map(t => math.abs(t - tParalelo)))
        val razones = muestras.map { case (_, s, p) => aceleracion(s, p) }
        println(f"$n%d | $u%d | $tSecuencial%.6f | $tParalelo%.6f | ${aceleracion(tSecuencial, tParalelo)}%.6f | $madS%.6f | $madP%.6f | ${razones.min}%.6f | ${razones.max}%.6f")
        (u, tParalelo)
      }
      println(s"Mejor umbral observado para n = $n: ${mediciones.minBy(_._2)._1}")
    }
  }
}
