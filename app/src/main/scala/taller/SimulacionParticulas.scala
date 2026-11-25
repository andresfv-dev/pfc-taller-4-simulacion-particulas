package taller

import common._
import org.scalameter._

/**
 * Taller 4 — simulación de partículas repartida entre varios núcleos.
 *
 * Complete cada función. Las firmas y los tipos no se modifican.
 *
 * Las abstracciones `parallel` y `task` vienen en el paquete `common`, que ya
 * está en el proyecto.
 */
class SimulacionParticulas {

  type Sistema = Array[Particula]

  // Punto 1: un paso de la simulación -----------------------------------------

  /** La posición nueva es la anterior más la velocidad por dt. */
  def mover(p: Particula, dt: Double): Particula = {
    Particula(p.pos + p.vel*dt, p.vel,p.masa)
  }

  /** Avanza todo el sistema un paso de tiempo dt. */
  def pasoSecuencial(s: Sistema, dt: Double): Sistema = {
    s.map(p => mover(p, dt))
  }

  /** Avanza solo el tramo entre `desde` incluido y `hasta` excluido. */
  def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
    s.slice(desde, hasta).map(p => mover(p, dt))
  }

  /**
   * Parte el sistema por la mitad con `parallel` mientras el tramo tenga más
   * elementos que el umbral, y por debajo de esa cifra resuelve en secuencial.
   */
  def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
    val umb = math.max(umbral, 1)
    def resolver(desde: Int, hasta: Int): Sistema =
      if (hasta - desde <= umb) pasoSegmento(s, dt, desde, hasta)
      else {
        val medio = desde + (hasta - desde) / 2
        val (izq, der) = parallel(resolver(desde, medio), resolver(medio, hasta))
        izq ++ der
      }
    if (s.length <= umb) pasoSecuencial(s, dt) else resolver(0, s.length)
  }

  // Punto 2: magnitudes globales ----------------------------------------------

  /** La suma de las energías cinéticas, media masa por velocidad al cuadrado. */
  def energiaSecuencial(s: Sistema): Double = {
    s.map(p => 0.5 * p.masa * p.vel.norma * p.vel.norma).sum
  }

  /** La suma de las masas. */
  def masaTotal(s: Sistema): Double = {
    s.map(p => p.masa).sum
  }

  /**
   * La suma de las posiciones ponderadas por la masa, dividida entre la masa
   * total. Sin masa, el origen.
   */
  def centroDeMasaSecuencial(s: Sistema): Vector2 = {
    val m = masaTotal(s)
    if (m == 0.0) Vector2(0, 0)
    else {
      val suma = s.foldLeft(Vector2(0, 0))((acc, p) => acc + p.pos * p.masa)
      suma * (1.0 / m)
    }
  }

  // Punto 3: las mismas magnitudes en paralelo --------------------------------

  /** La energía del sistema, repartida con el mismo umbral del punto 1. */
  def energiaParalela(s: Sistema, umbral: Int): Double = {
    val umb = math.max(umbral, 1)
    def resolver(desde: Int, hasta: Int): Double =
      if (hasta - desde <= umb) energiaSecuencial(s.slice(desde, hasta))
      else {
        val medio = desde + (hasta - desde) / 2
        val (izq, der) = parallel(resolver(desde, medio), resolver(medio, hasta))
        izq + der
      }
    resolver(0, s.length)
  }

  /**
   * El centro de masa, repartido. Promediar los promedios de cada mitad da
   * otra cosa cuando las dos mitades no pesan igual.
   */
  def centroDeMasaParalelo(s: Sistema, umbral: Int): Vector2 = {
    val umb = math.max(umbral, 1)
    def resolver(desde: Int, hasta: Int): (Vector2, Double) =
      if (hasta - desde <= umb) {
        s.slice(desde, hasta).foldLeft((Vector2(0, 0), 0.0)) {
          case ((suma, masa), p) => (suma + p.pos * p.masa, masa + p.masa)
        }
      } else {
        val medio = desde + (hasta - desde) / 2
        val (izq, der) = parallel(resolver(desde, medio), resolver(medio, hasta))
        (izq._1 + der._1, izq._2 + der._2)
      }
    val (suma, masa) = resolver(0, s.length)
    if (masa == 0.0) Vector2(0, 0) else suma * (1.0 / masa)
  }

  // Punto 4: trabajo y profundidad --------------------------------------------

  /** El número total de operaciones, una por partícula. */
  def trabajo(n: Int): Int = {
    n
  }

  /** Cuántas veces se parte a la mitad, más el tramo que queda sin partir. */
  def profundidad(n: Int, umbral: Int): Int = {
    val umb = math.max(umbral, 1)
    def resolver(tamano: Int): Int =
      if (tamano <= umb) tamano
      else {
        val medio = tamano / 2
        1 + math.max(resolver(medio), resolver(tamano - medio))
      }
    resolver(n)
  }

  /** La profundidad más el trabajo entre p procesadores. */
  def tiempoEstimado(n: Int, p: Int, umbral: Int): Double = {
    profundidad(n, umbral) + trabajo(n).toDouble / p
  }

  // Punto 5: medir la aceleración ---------------------------------------------

  /** Mediana de varias mediciones de ScalaMeter, en milisegundos. */
  def medir[A](repeticiones: Int)(cuerpo: => A): Double = {
    val muestras = Vector.fill(repeticiones) {
      (withWarmer(new Warmer.Default) measure { cuerpo }).value
    }
    muestras.sorted.apply(muestras.length / 2)
  }

  /** El cociente entre el tiempo secuencial y el paralelo. */
  def aceleracion(tSecuencial: Double, tParalelo: Double): Double = {
    tSecuencial / tParalelo
  }

  /**
   * El umbral que menos tarda de una lista de candidatos. Con la lista vacía,
   * el tamaño del sistema, que equivale a no repartir.
   */
  def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int = {
    if (umbrales.isEmpty) s.length
    else umbrales.map(u => (u, medir(3) { pasoParalelo(s, dt, u) })).minBy(_._2)._1
  }
}
