package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.util.Random

/**
 * Las pruebas no miden tiempos: un tiempo depende de la máquina y de lo que
 * esté corriendo al lado, así que una prueba que lo compare fallaría sin que
 * nada esté mal.
 */
@RunWith(classOf[JUnitRunner])
class SimulacionParticulasTest extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  val eps = 1e-9

  val p = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)

  val s: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(3, 4), 2.0), // norma 5
    Particula(Vector2(4, 0), Vector2(0, 0), 2.0))

  val vacio: Sistema = Array()

  val azar = new Random(7)
  val grande: Sistema = Array.fill(1000)(Particula(
    Vector2(azar.nextDouble() * 10, azar.nextDouble() * 10),
    Vector2(azar.nextDouble() * 2, azar.nextDouble() * 2),
    azar.nextDouble() * 5 + 0.1))

  def cerca(a: Double, b: Double): Boolean = Math.abs(a - b) < 1e-6

  def cerca(a: Vector2, b: Vector2): Boolean =
    Math.abs(a.x - b.x) < 1e-6 && Math.abs(a.y - b.y) < 1e-6

  // Punto 1 -------------------------------------------------------------------

  test("pasoSecuencial: la posición avanza velocidad por dt") {
    assert(pasoSecuencial(Array(p), 2.0).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoSecuencial: con dt cero nada se mueve") {
    assert(pasoSecuencial(Array(p), 0.0).head.pos == Vector2(0.0, 0.0))
  }

  test("pasoSecuencial: la velocidad y la masa no cambian") {
    val despues = pasoSecuencial(Array(p), 3.0).head
    assert(despues.vel == p.vel && despues.masa == p.masa)
  }

  test("pasoSecuencial: sobre el sistema vacío no hay nada que mover") {
    assert(pasoSecuencial(vacio, 1.0).isEmpty)
  }

  test("pasoSegmento: solo calcula el tramo pedido") {
    val tramo = pasoSegmento(s, 1.0, 1, 2)
    assert(tramo.length == 1 && tramo.head.pos == Vector2(4.0, 0.0))
  }

  test("pasoSegmento: el tramo vacío da un sistema vacío") {
    assert(pasoSegmento(s, 1.0, 1, 1).isEmpty)
  }

  test("pasoParalelo: da lo mismo que el secuencial en un caso simple") {
    assert(pasoParalelo(Array(p), 2.0, 1).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoParalelo: coincide con el secuencial para varios umbrales") {
    val umbrales = List(1, 2, 7, 64, 1000, 5000)
    assert(umbrales.forall { u =>
      pasoParalelo(grande, 0.5, u).zip(pasoSecuencial(grande, 0.5))
        .forall { case (a, b) => cerca(a.pos, b.pos) }
    })
  }

  test("pasoParalelo: conserva el orden y el tamaño del sistema") {
    assert(pasoParalelo(grande, 0.5, 32).length == grande.length)
  }

  test("pasoParalelo: con umbral cero no se queda sin fondo") {
    assert(pasoParalelo(Array(p), 1.0, 0).length == 1)
  }

  // Punto 2 -------------------------------------------------------------------

  test("energiaSecuencial: media masa por velocidad al cuadrado") {
    assert(cerca(energiaSecuencial(s), 25.0))
  }

  test("energiaSecuencial: el sistema vacío no tiene energía") {
    assert(energiaSecuencial(vacio) == 0.0)
  }

  test("energiaSecuencial: una partícula quieta no aporta energía") {
    assert(cerca(energiaSecuencial(Array(s(1))), 0.0))
  }

  test("masaTotal: suma las masas") {
    assert(cerca(masaTotal(s), 4.0))
  }

  test("masaTotal: el sistema vacío no tiene masa") {
    assert(masaTotal(vacio) == 0.0)
  }

  test("centroDeMasaSecuencial: el punto medio de dos masas iguales") {
    assert(cerca(centroDeMasaSecuencial(s), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaSecuencial: sin masa el centro es el origen") {
    assert(centroDeMasaSecuencial(vacio) == Vector2(0, 0))
  }

  test("centroDeMasaSecuencial: la masa mayor arrastra el centro") {
    val pesado: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 3.0),
      Particula(Vector2(4, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaSecuencial(pesado), Vector2(1.0, 0.0)))
  }

  // Punto 3 -------------------------------------------------------------------

  test("energiaParalela: coincide con la secuencial en el ejemplo") {
    assert(cerca(energiaParalela(s, 1), 25.0))
  }

  test("energiaParalela: coincide con la secuencial para varios umbrales") {
    val esperado = energiaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(energiaParalela(grande, u), esperado)))
  }

  test("energiaParalela: sobre el sistema vacío da cero") {
    assert(energiaParalela(vacio, 1) == 0.0)
  }

  test("centroDeMasaParalelo: coincide con el secuencial en el ejemplo") {
    assert(cerca(centroDeMasaParalelo(s, 1), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaParalelo: coincide con el secuencial para varios umbrales") {
    val esperado = centroDeMasaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(centroDeMasaParalelo(grande, u), esperado)))
  }

  test("centroDeMasaParalelo: promediar promedios daría otra cosa") {
    // Tres partículas: dos en el origen y una lejos. El promedio de los
    // promedios de las dos mitades no es el centro de masa del conjunto.
    val tres: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(9, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaParalelo(tres, 1), Vector2(3.0, 0.0)))
  }

  test("centroDeMasaParalelo: sin masa el centro es el origen") {
    assert(centroDeMasaParalelo(vacio, 1) == Vector2(0, 0))
  }

  // Punto 4 -------------------------------------------------------------------

  test("trabajo: una operación por partícula") { assert(trabajo(1024) == 1024) }

  test("profundidad: si el umbral cubre todo, no se parte") {
    assert(profundidad(1024, 1024) == 1024)
  }

  test("profundidad: una partición y el tramo que queda") {
    assert(profundidad(1024, 512) == 513)
  }

  test("profundidad: diez particiones y un elemento") {
    assert(profundidad(1024, 1) == 11)
  }

  test("tiempoEstimado: profundidad más trabajo entre procesadores") {
    assert(cerca(tiempoEstimado(1024, 4, 1), 267.0))
  }

  test("tiempoEstimado: con un procesador no hay ganancia") {
    assert(cerca(tiempoEstimado(1024, 1, 1), 11.0 + 1024.0))
  }

  test("profundidad: nunca supera el trabajo") {
    val casos = for (n <- List(1, 7, 64, 1024); u <- List(1, 8, 64)) yield (n, u)
    assert(casos.forall { case (n, u) => profundidad(n, u) <= trabajo(n) })
  }

  // Punto 5 -------------------------------------------------------------------

  test("aceleracion: cuatro veces más rápido da 4.0") {
    assert(cerca(aceleracion(100.0, 25.0), 4.0))
  }

  test("aceleracion: el mismo tiempo da 1.0") {
    assert(cerca(aceleracion(100.0, 100.0), 1.0))
  }

  test("umbralConveniente: sin candidatos devuelve el tamaño del sistema") {
    assert(umbralConveniente(grande, 0.5, Nil) == grande.length)
  }

  test("umbralConveniente: devuelve uno de los candidatos") {
    val candidatos = List(16, 128, 1024)
    assert(candidatos.contains(umbralConveniente(grande, 0.5, candidatos)))
  }

  // Pruebas propia

  test("mover: con velocidad negativa la partícula retrocede") {
    val p = Particula(Vector2(5, 5), Vector2(-2, -1), 1.0)
    assert(mover(p, 2.0).pos == Vector2(1, 3))
  }

  test("mover: con dt = 0 la partícula se queda donde estaba") {
    val p = Particula(Vector2(7, -3), Vector2(10, 10), 2.0)
    assert(mover(p, 0.0).pos == Vector2(7, -3))
  }

  test("mover: la velocidad y la masa no cambian") {
    val p = Particula(Vector2(0, 0), Vector2(3, 4), 5.0)
    val q = mover(p, 3.0)
    assert(q.vel == Vector2(3, 4) && q.masa == 5.0)
  }

  test("mover: funciona con un dt decimal") {
    val p = Particula(Vector2(1, 1), Vector2(4, -2), 1.0)
    assert(mover(p, 0.5).pos == Vector2(3, 0))
  }

  test("mover: dos pasos de dt = 1 equivalen a un paso de dt = 2") {
    val p = Particula(Vector2(0, 0), Vector2(3, 1), 1.0)
    assert(mover(mover(p, 1.0), 1.0) == mover(p, 2.0))
  }
  // pruebas propias pasoSecuencial

  val tres: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(1, 0), 1.0),
    Particula(Vector2(2, 2), Vector2(0, -1), 2.0),
    Particula(Vector2(-1, 3), Vector2(2, 2), 3.0))

  test("pasoSecuencial: cada partícula se mueve con su propia velocidad") {
    val r = pasoSecuencial(tres, 2.0)
    assert(r(0).pos == Vector2(2, 0) && r(1).pos == Vector2(2, 0) && r(2).pos == Vector2(3, 7))
  }

  test("pasoSecuencial: el sistema conserva su tamaño") {
    assert(pasoSecuencial(tres, 1.0).length == 3)
  }

  test("pasoSecuencial: las partículas quedan en el mismo orden") {
    assert(pasoSecuencial(tres, 1.0).map(_.masa).toList == List(1.0, 2.0, 3.0))
  }

  test("pasoSecuencial: no modifica el sistema original") {
    pasoSecuencial(tres, 5.0)
    assert(tres(0).pos == Vector2(0, 0) && tres(2).pos == Vector2(-1, 3))
  }

  test("pasoSecuencial: si ninguna partícula tiene velocidad el sistema queda igual") {
    val quietas: Sistema = Array(
      Particula(Vector2(1, 1), Vector2(0, 0), 1.0),
      Particula(Vector2(4, -2), Vector2(0, 0), 2.0))
    assert(pasoSecuencial(quietas, 10.0).toList == quietas.toList)
  }

  // pruebas propias pasoSegmento

  test("pasoSegmento: un tramo intermedio mueve solo sus partículas") {
    val r = pasoSegmento(tres, 2.0, 1, 3)
    assert(r.length == 2 && r(0).pos == Vector2(2, 0) && r(1).pos == Vector2(3, 7))
  }

  test("pasoSegmento: el tramo completo coincide con pasoSecuencial") {
    assert(pasoSegmento(tres, 2.0, 0, 3).toList == pasoSecuencial(tres, 2.0).toList)
  }

  test("pasoSegmento: dos tramos contiguos unidos dan el paso completo") {
    assert((0 to 3).forall { m =>
      (pasoSegmento(tres, 1.5, 0, m) ++ pasoSegmento(tres, 1.5, m, 3)).toList ==
        pasoSecuencial(tres, 1.5).toList
    })
  }

  test("pasoSegmento: el resultado tiene hasta - desde partículas") {
    assert(pasoSegmento(grande, 0.5, 100, 350).length == 250)
  }

  test("pasoSegmento: no modifica el sistema original") {
    pasoSegmento(tres, 5.0, 0, 3)
    assert(tres(0).pos == Vector2(0, 0) && tres(1).pos == Vector2(2, 2) && tres(2).pos == Vector2(-1, 3))
  }

  // pruebas propias pasoParalelo

  test("pasoParalelo: con umbral 1 parte hasta cada partícula y da lo mismo que el secuencial") {
    assert(pasoParalelo(tres, 2.0, 1).toList == pasoSecuencial(tres, 2.0).toList)
  }

  test("pasoParalelo: con umbral mayor que el sistema no parte y da lo mismo que el secuencial") {
    assert(pasoParalelo(tres, 2.0, 100).toList == pasoSecuencial(tres, 2.0).toList)
  }

  test("pasoParalelo: sobre el sistema vacío da un sistema vacío") {
    assert(pasoParalelo(vacio, 1.0, 4).isEmpty)
  }

  test("pasoParalelo: con umbral negativo no se queda sin fondo") {
    assert(pasoParalelo(tres, 1.0, -5).toList == pasoSecuencial(tres, 1.0).toList)
  }

  test("pasoParalelo: con un umbral impar conserva el orden de las partículas") {
    assert(pasoParalelo(grande, 0.5, 3).map(_.masa).toList == grande.map(_.masa).toList)
  }

  // pruebas propias energiaSecuencial

  test("energiaSecuencial: suma la energía de cada partícula") {
    // 0.5*1*1 + 0.5*2*1 + 0.5*3*8 = 0.5 + 1 + 12
    assert(cerca(energiaSecuencial(tres), 13.5))
  }

  test("energiaSecuencial: la dirección de la velocidad no importa") {
    val haciaAtras = Particula(Vector2(0, 0), Vector2(-3, -4), 2.0)
    assert(cerca(energiaSecuencial(Array(haciaAtras)), 25.0))
  }

  test("energiaSecuencial: el doble de masa da el doble de energía") {
    val liviana = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)
    val pesada = Particula(Vector2(0, 0), Vector2(1, 2), 2.0)
    assert(cerca(energiaSecuencial(Array(pesada)), 2 * energiaSecuencial(Array(liviana))))
  }

  test("energiaSecuencial: el doble de velocidad da cuatro veces la energía") {
    val lenta = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)
    val rapida = Particula(Vector2(0, 0), Vector2(2, 4), 1.0)
    assert(cerca(energiaSecuencial(Array(rapida)), 4 * energiaSecuencial(Array(lenta))))
  }

  test("energiaSecuencial: un paso de la simulación no cambia la energía") {
    assert(cerca(energiaSecuencial(pasoSecuencial(tres, 3.0)), energiaSecuencial(tres)))
  }

  // pruebas propias masaTotal

  test("masaTotal: suma las masas de tres") {
    assert(cerca(masaTotal(tres), 6.0))
  }

  test("masaTotal: funciona con masas decimales") {
    val sistema: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 0.5),
      Particula(Vector2(1, 1), Vector2(0, 0), 0.25),
      Particula(Vector2(2, 2), Vector2(0, 0), 0.25))
    assert(cerca(masaTotal(sistema), 1.0))
  }

  test("masaTotal: no depende de las posiciones ni de las velocidades") {
    assert(cerca(masaTotal(pasoSecuencial(tres, 5.0)), masaTotal(tres)))
  }

  test("masaTotal: la masa de dos partes suma la masa del todo") {
    assert(cerca(masaTotal(tres.slice(0, 1)) + masaTotal(tres.slice(1, 3)), masaTotal(tres)))
  }

  test("masaTotal: en grande queda entre 1000 veces la mínima y la máxima") {
    val m = masaTotal(grande)
    assert(m >= 1000 * 0.1 && m <= 1000 * 5.1)
  }

  // pruebas propias centroDeMasaSecuencial

  test("centroDeMasaSecuencial: el centro de tres con masas distintas") {
    // (1*(0,0) + 2*(2,2) + 3*(-1,3)) / 6 = (1, 13) / 6
    assert(cerca(centroDeMasaSecuencial(tres), Vector2(1.0 / 6, 13.0 / 6)))
  }

  test("centroDeMasaSecuencial: con una sola partícula el centro es su posición") {
    val una = Particula(Vector2(3, -2), Vector2(1, 1), 5.0)
    assert(cerca(centroDeMasaSecuencial(Array(una)), Vector2(3, -2)))
  }

  test("centroDeMasaSecuencial: si todas las masas son cero el centro es el origen") {
    val sinMasa: Sistema = Array(
      Particula(Vector2(5, 5), Vector2(0, 0), 0.0),
      Particula(Vector2(-2, 7), Vector2(0, 0), 0.0))
    assert(centroDeMasaSecuencial(sinMasa) == Vector2(0, 0))
  }

  test("centroDeMasaSecuencial: multiplicar todas las masas por 10 no cambia el centro") {
    val diezVeces = tres.map(p => Particula(p.pos, p.vel, p.masa * 10))
    assert(cerca(centroDeMasaSecuencial(diezVeces), centroDeMasaSecuencial(tres)))
  }

  test("centroDeMasaSecuencial: si todas se mueven igual el centro se mueve igual") {
    val juntas = tres.map(p => Particula(p.pos, Vector2(1, 1), p.masa))
    val antes = centroDeMasaSecuencial(juntas)
    val despues = centroDeMasaSecuencial(pasoSecuencial(juntas, 2.0))
    assert(cerca(despues, antes + Vector2(2, 2)))
  }
}

@RunWith(classOf[JUnitRunner])
class SimulacionParticulasPuntos3y4Test extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  def cerca(a: Double, b: Double): Boolean = Math.abs(a - b) < 1e-6

  def cerca(a: Vector2, b: Vector2): Boolean =
    Math.abs(a.x - b.x) < 1e-6 && Math.abs(a.y - b.y) < 1e-6

  // pruebas propias punto 3

  test("energiaParalela: cinco partículas con masas y velocidades distintas") {
    val sistema: Sistema = Array(
      Particula(Vector2(1, 2), Vector2(-2, 0), 0.5),
      Particula(Vector2(3, 1), Vector2(0, 3), 2.0),
      Particula(Vector2(0, 4), Vector2(1, 2), 4.0),
      Particula(Vector2(-1, 2), Vector2(7, 0), 0.0),
      Particula(Vector2(2, -3), Vector2(0, 0), 3.0))
    // 1 + 9 + 10 + 0 + 0 = 20
    assert(List(-4, 0, 1, 2, 5, 9).forall(u => cerca(energiaParalela(sistema, u), 20.0)))
  }

  test("energiaParalela: cambiar las posiciones no cambia la energía ni el original") {
    val sistema: Sistema = Array(
      Particula(Vector2(-2, 1), Vector2(0, -2), 3.0),
      Particula(Vector2(4, 3), Vector2(4, 0), 0.25))
    val original = sistema.toList
    val trasladado = sistema.map(p => Particula(p.pos + Vector2(10, -7), p.vel, p.masa))
    assert(cerca(energiaParalela(sistema, 1), 8.0))
    assert(cerca(energiaParalela(trasladado, 1), 8.0))
    assert(sistema.toList == original)
  }

  test("centroDeMasaParalelo: combina masas distintas en las dos coordenadas") {
    val sistema: Sistema = Array(
      Particula(Vector2(-4, 2), Vector2(1, 0), 2.0),
      Particula(Vector2(2, -2), Vector2(0, 1), 1.0),
      Particula(Vector2(6, 4), Vector2(-1, 0), 3.0))
    // Suma ponderada (12, 14), masa 6
    assert(List(-3, 0, 1, 2, 3, 8).forall { u =>
      cerca(centroDeMasaParalelo(sistema, u), Vector2(2.0, 7.0 / 3.0))
    })
  }

  test("centroDeMasaParalelo: una mitad sin masa no altera la otra") {
    val sistema: Sistema = Array(
      Particula(Vector2(100, -100), Vector2(0, 0), 0.0),
      Particula(Vector2(-50, 80), Vector2(0, 0), 0.0),
      Particula(Vector2(-3, 5), Vector2(0, 0), 0.5),
      Particula(Vector2(1, 1), Vector2(0, 0), 1.5))
    val original = sistema.toList
    assert(cerca(centroDeMasaParalelo(sistema, 2), Vector2(0, 2)))
    assert(sistema.toList == original)
  }

  test("centroDeMasaParalelo: partículas sin masa lejos del origen dan el origen") {
    val sistema: Sistema = Array(
      Particula(Vector2(8, -2), Vector2(1, 1), 0.0),
      Particula(Vector2(-6, 9), Vector2(2, 0), 0.0),
      Particula(Vector2(3, 7), Vector2(0, 4), 0.0))
    assert(List(0, 1, 2, 7).forall(u => centroDeMasaParalelo(sistema, u) == Vector2(0, 0)))
  }

  // pruebas propias punto 4

  test("trabajo: el sistema vacío y un tamaño grande conservan su número de operaciones") {
    assert(trabajo(0) == 0 && trabajo(Int.MaxValue) == Int.MaxValue)
  }

  test("profundidad: la mitad de once puede costar más que la de doce") {
    assert(profundidad(11, 5) == 6 && profundidad(12, 5) == 5)
    assert(profundidad(23, 5) == 7)
  }

  test("profundidad: un tamaño impar conserva el elemento sobrante al dividir") {
    assert(profundidad(9, 2) == 5)
  }

  test("profundidad: el vacío cuesta cero y los umbrales no positivos terminan") {
    assert(List(-2, 0, 1, 4).forall(u => profundidad(0, u) == 0))
    assert(List(-2, 0, 1).forall(u => profundidad(5, u) == 4))
  }

  test("tiempoEstimado: conserva la fracción del trabajo entre procesadores") {
    assert(cerca(tiempoEstimado(13, 8, 3), 6.625))
    assert(cerca(tiempoEstimado(0, 3, 2), 0.0))
  }
}

@RunWith(classOf[JUnitRunner])
class SimulacionParticulasPunto5Test extends AnyFunSuite {

  val sim = new SimulacionParticulas()

  test("aceleracion: una mejora decimal conserva la fracción") {
    assert(math.abs(sim.aceleracion(7.5, 3.0) - 2.5) < 1e-9)
  }

  test("aceleracion: un paralelo más lento da menos de uno") {
    assert(sim.aceleracion(12.0, 48.0) == 0.25)
  }

  test("aceleracion: cambiar la unidad de ambos tiempos conserva el cociente") {
    assert(math.abs(sim.aceleracion(18.0, 6.0) - sim.aceleracion(0.018, 0.006)) < 1e-9)
  }

  test("umbralConveniente: sin candidatos devuelve el tamaño, vacío o impar, sin medir") {
    val sinMedicion = new SimulacionParticulas() {
      override def medir[A](repeticiones: Int)(cuerpo: => A): Double =
        fail("La lista vacía no necesita mediciones")
    }
    assert(sinMedicion.umbralConveniente(Array.empty[Particula], 0.5, Nil) == 0)
    val sistema = Array.fill(7)(Particula(Vector2(1, 2), Vector2(3, 4), 2.0))
    assert(sinMedicion.umbralConveniente(sistema, 2.0, Nil) == 7)
  }

  test("umbralConveniente: mide el paso completo con el sistema y dt recibidos") {
    val sistema = Array(
      Particula(Vector2(1, 2), Vector2(-2, 3), 2.0),
      Particula(Vector2(-4, 0), Vector2(1, -1), 3.0))
    val original = sistema.toList
    val comprobacion = new SimulacionParticulas() {
      override def medir[A](repeticiones: Int)(cuerpo: => A): Double = {
        assert(repeticiones == 3)
        assert(cuerpo.asInstanceOf[Sistema].toList == List(
          Particula(Vector2(-3, 8), Vector2(-2, 3), 2.0),
          Particula(Vector2(-2, -2), Vector2(1, -1), 3.0)))
        1.0
      }
    }
    assert(comprobacion.umbralConveniente(sistema, 2.0, List(1)) == 1)
    assert(sistema.toList == original)
  }

}
