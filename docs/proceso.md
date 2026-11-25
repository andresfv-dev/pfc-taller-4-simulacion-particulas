# Informe de proceso

## 1. `mover`

### Definición del algoritmo

```scala
def mover(p: Particula, dt: Double): Particula = {
  Particula(p.pos + p.vel * dt, p.vel, p.masa)
}
```

* La función `mover` calcula dónde queda una partícula después de un intervalo
  de tiempo `dt`.
* Recibe dos parámetros:

  * `p`: la partícula, con su posición `pos`, su velocidad `vel` y su `masa`.
  * `dt`: el intervalo de tiempo que avanza la simulación.
* No modifica `p`: construye una partícula nueva. La posición cambia; la
  velocidad y la masa se copian tal cual.
* La función no es recursiva: es una sola expresión que se evalúa en un
  número fijo de pasos, sin importar los datos de entrada.

### Explicación paso a paso

La posición nueva sigue la ecuación del movimiento rectilíneo uniforme:

```math
\vec{p}\,' = \vec{p} + \vec{v} \cdot dt
```

Las operaciones `+` y `*` son métodos de `Vector2`, definidos en `Modelo.scala`:

```scala
def +(o: Vector2): Vector2 = Vector2(x + o.x, y + o.y)
def *(k: Double): Vector2 = Vector2(x * k, y * k)
```

Por la precedencia de operadores de Scala, `*` se evalúa antes que `+`:

```scala
p.pos + p.vel * dt   ==   p.pos.+(p.vel.*(dt))
```

Entonces, la evaluación ocurre en este orden:

1. `p.vel * dt`: se escala la velocidad por el tiempo (el desplazamiento).
2. `p.pos + (...)`: se suma el desplazamiento a la posición.
3. `Particula(...)`: se construye la partícula nueva con la posición nueva,
   la misma velocidad y la misma masa.

---

### Evaluación con el modelo de sustitución

Ejemplo:

```scala
val p = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)
mover(p, 2.0)
```

#### Paso 1: Llamada inicial

```scala
mover(Particula(Vector2(0, 0), Vector2(1, 2), 1.0), 2.0)
```

#### Paso 2: Se reemplaza el cuerpo de la función

```scala
Particula(Vector2(0, 0) + Vector2(1, 2) * 2.0, Vector2(1, 2), 1.0)
```

#### Paso 3: Se evalúa `*` (desplazamiento)

```scala
Vector2(1, 2) * 2.0  →  Vector2(1 * 2.0, 2 * 2.0)  →  Vector2(2.0, 4.0)
```

#### Paso 4: Se evalúa `+` (posición nueva)

```scala
Vector2(0, 0) + Vector2(2.0, 4.0)  →  Vector2(0 + 2.0, 0 + 4.0)  →  Vector2(2.0, 4.0)
```

#### Paso 5: Se construye la partícula resultado

```scala
Particula(Vector2(2.0, 4.0), Vector2(1, 2), 1.0)
```

---

### Pila de llamados

Como `mover` no es recursiva, la pila nunca crece más allá de tres niveles:
la llamada a `mover`, y dentro de ella, una llamada a `*` y luego una a `+`.
Cada una termina antes de que empiece la siguiente.

```mermaid
sequenceDiagram
    participant M as mover(p, 2.0)
    participant Por as Vector2(1,2) * 2.0
    participant Mas as Vector2(0,0) + Vector2(2,4)
    participant C as Particula(...)

    M->>Por: calcula el desplazamiento
    Por-->>M: Vector2(2.0, 4.0)
    M->>Mas: suma a la posición
    Mas-->>M: Vector2(2.0, 4.0)
    M->>C: construye la partícula nueva
    C-->>M: Particula(Vector2(2,4), Vector2(1,2), 1.0)
```

---

### Ejemplo de uso

```scala
val p = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)
val q = mover(p, 2.0)
println(q.pos)  // Vector2(2.0,4.0)
println(p.pos)  // Vector2(0.0,0.0), p no cambió
```

El resultado de `mover(p, 2.0)` es una partícula en `(2, 4)` con la misma
velocidad `(1, 2)` y la misma masa `1.0`. La partícula original sigue igual.

---

## 2. `pasoSecuencial`

### Definición del algoritmo

```scala
def pasoSecuencial(s: Sistema, dt: Double): Sistema = {
  s.map(p => mover(p, dt))
}
```

* La función `pasoSecuencial` avanza todo el sistema un intervalo de
  tiempo `dt`.
* Recibe dos parámetros:

  * `s`: el sistema, que es un `Array[Particula]`.
  * `dt`: el intervalo de tiempo.
* Usa `map`, que aplica una función a cada elemento del arreglo y devuelve un
  arreglo nuevo del mismo tamaño, con los resultados en el mismo orden.
* La función que se aplica es la lambda `p => mover(p, dt)`: "a cada
  partícula `p`, muévela `dt`". Por eso `pasoSecuencial` reutiliza `mover`.
* No modifica `s`: el arreglo original queda igual.

### Explicación paso a paso

`map` se comporta como si recorriera el arreglo de izquierda a derecha:

1. Toma la partícula en la posición `0` y la mueve.
2. Toma la partícula en la posición `1` y la mueve.
3. … y así hasta la última.
4. Pone los resultados en un arreglo nuevo, en el mismo orden.

Si `s` está vacío no hay nada que recorrer y el resultado es un arreglo vacío.

Todo esto ocurre en un solo hilo, una partícula después de otra. Por eso
se llama secuencial: el número de pasos crece con el número de partículas
$n$.

---

### Evaluación con el modelo de sustitución

Ejemplo:

```scala
val tres: Sistema = Array(
  Particula(Vector2(0, 0), Vector2(1, 0), 1.0),
  Particula(Vector2(2, 2), Vector2(0, -1), 2.0),
  Particula(Vector2(-1, 3), Vector2(2, 2), 3.0))

pasoSecuencial(tres, 2.0)
```

#### Paso 1: Llamada inicial

```scala
pasoSecuencial(tres, 2.0)
```

#### Paso 2: Se reemplaza el cuerpo de la función

```scala
tres.map(p => mover(p, 2.0))
```

#### Paso 3: `map` aplica `mover` a cada partícula

```scala
Array(
  mover(Particula(Vector2(0, 0),  Vector2(1, 0),  1.0), 2.0),
  mover(Particula(Vector2(2, 2),  Vector2(0, -1), 2.0), 2.0),
  mover(Particula(Vector2(-1, 3), Vector2(2, 2),  3.0), 2.0))
```

#### Paso 4: Se evalúa cada `mover` (como en la sección 1)

```scala
(0, 0)  + (1, 0)  * 2.0  =  (0, 0)  + (2, 0)   =  (2, 0)
(2, 2)  + (0, -1) * 2.0  =  (2, 2)  + (0, -2)  =  (2, 0)
(-1, 3) + (2, 2)  * 2.0  =  (-1, 3) + (4, 4)   =  (3, 7)
```

#### Paso 5: Resultado

```scala
Array(
  Particula(Vector2(2, 0), Vector2(1, 0),  1.0),
  Particula(Vector2(2, 0), Vector2(0, -1), 2.0),
  Particula(Vector2(3, 7), Vector2(2, 2),  3.0))
```

Mismo tamaño (3), mismo orden (masas 1, 2, 3), velocidades y masas iguales;
solo cambiaron las posiciones.

---

### Diagrama de llamados

`map` llama a `mover` una vez por partícula, una después de otra. Cada llamado
termina antes de que empiece el siguiente, así que la pila no crece con el
tamaño del sistema.

```mermaid
sequenceDiagram
    participant PS as pasoSecuencial(tres, 2.0)
    participant Map as tres.map
    participant M0 as mover(tres(0), 2.0)
    participant M1 as mover(tres(1), 2.0)
    participant M2 as mover(tres(2), 2.0)

    PS->>Map: aplica p => mover(p, 2.0)
    Map->>M0: partícula 0
    M0-->>Map: pos (2, 0)
    Map->>M1: partícula 1
    M1-->>Map: pos (2, 0)
    Map->>M2: partícula 2
    M2-->>Map: pos (3, 7)
    Map-->>PS: Array(nuevas partículas)
```

---

### Ejemplo de uso

```scala
val r = pasoSecuencial(tres, 2.0)
println(r.map(_.pos).toList)     // List(Vector2(2.0,0.0), Vector2(2.0,0.0), Vector2(3.0,7.0))
println(tres.map(_.pos).toList)  // List(Vector2(0.0,0.0), Vector2(2.0,2.0), Vector2(-1.0,3.0)), tres no cambió
```

---

## 3. `pasoSegmento`

### Definición del algoritmo

```scala
def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
  s.slice(desde, hasta).map(p => mover(p, dt))
}
```

* La función `pasoSegmento` avanza un intervalo `dt` solo un tramo del
  sistema y devuelve únicamente ese tramo.
* Recibe cuatro parámetros:

  * `s`: el sistema completo, un `Array[Particula]`.
  * `dt`: el intervalo de tiempo.
  * `desde`: la posición donde empieza el tramo (incluida).
  * `hasta`: la posición donde termina el tramo (excluida).
* El resultado tiene `hasta - desde` partículas. Si `desde == hasta`, el
  tramo es vacío y el resultado también.
* Es el bloque con el que se construye `pasoParalelo`: cada hilo resuelve un
  tramo con `pasoSegmento` y luego se pegan los resultados.
* No modifica `s`.

### Explicación paso a paso

La función hace dos cosas, una después de la otra:

1. `s.slice(desde, hasta)`: recorta el arreglo y se queda con las partículas
   en las posiciones `desde`, `desde + 1`, …, `hasta - 1`. Es un arreglo
   nuevo; `s` no cambia.
2. `.map(p => mover(p, dt))`: mueve cada partícula del recorte, igual que
   `pasoSecuencial` (sección 2), pero solo sobre el tramo.

Es decir, `pasoSegmento(s, dt, desde, hasta)` es lo mismo que
`pasoSecuencial(s.slice(desde, hasta), dt)`.

Si `desde` o `hasta` se salen del arreglo, `slice` los ajusta a los bordes en
vez de fallar, y si `hasta <= desde` devuelve un arreglo vacío.

---

### Evaluación con el modelo de sustitución

Ejemplo, con el sistema `tres` de la sección 2:

```scala
pasoSegmento(tres, 2.0, 1, 3)
```

#### Paso 1: Llamada inicial

```scala
pasoSegmento(tres, 2.0, 1, 3)
```

#### Paso 2: Se reemplaza el cuerpo de la función

```scala
tres.slice(1, 3).map(p => mover(p, 2.0))
```

#### Paso 3: Se evalúa `slice` (posiciones 1 y 2; la 0 queda fuera)

```scala
Array(
  Particula(Vector2(2, 2),  Vector2(0, -1), 2.0),
  Particula(Vector2(-1, 3), Vector2(2, 2),  3.0)).map(p => mover(p, 2.0))
```

#### Paso 4: `map` aplica `mover` a cada partícula del tramo

```scala
Array(
  mover(Particula(Vector2(2, 2),  Vector2(0, -1), 2.0), 2.0),
  mover(Particula(Vector2(-1, 3), Vector2(2, 2),  3.0), 2.0))
```

#### Paso 5: Se evalúa cada `mover`

```scala
(2, 2)  + (0, -1) * 2.0  =  (2, 2)  + (0, -2)  =  (2, 0)
(-1, 3) + (2, 2)  * 2.0  =  (-1, 3) + (4, 4)   =  (3, 7)
```

#### Paso 6: Resultado

```scala
Array(
  Particula(Vector2(2, 0), Vector2(0, -1), 2.0),
  Particula(Vector2(3, 7), Vector2(2, 2),  3.0))
```

Dos partículas (`3 - 1 = 2`), las mismas que da `pasoSecuencial(tres, 2.0)`
en las posiciones 1 y 2. La partícula 0 no se calcula.

#### Caso del tramo vacío

```scala
pasoSegmento(tres, 2.0, 1, 1)
→ tres.slice(1, 1).map(p => mover(p, 2.0))
→ Array().map(p => mover(p, 2.0))
→ Array()
```

---

### Diagrama de llamados

```mermaid
sequenceDiagram
    participant PSeg as pasoSegmento(tres, 2.0, 1, 3)
    participant Sl as tres.slice(1, 3)
    participant Map as tramo.map
    participant M1 as mover(tres(1), 2.0)
    participant M2 as mover(tres(2), 2.0)

    PSeg->>Sl: recorta posiciones 1 y 2
    Sl-->>PSeg: Array(tres(1), tres(2))
    PSeg->>Map: aplica p => mover(p, 2.0)
    Map->>M1: partícula 1
    M1-->>Map: pos (2, 0)
    Map->>M2: partícula 2
    M2-->>Map: pos (3, 7)
    Map-->>PSeg: Array(2 partículas nuevas)
```

---

### Partir el sistema en tramos

Si se parte `tres` en el índice 1, los dos tramos juntos dan lo mismo que el
paso secuencial completo, sin perder ni repetir partículas:

```scala
pasoSegmento(tres, 2.0, 0, 1) ++ pasoSegmento(tres, 2.0, 1, 3)
// Array(pos (2,0)) ++ Array(pos (2,0), pos (3,7))
// == pasoSecuencial(tres, 2.0)
```

Esto es lo que aprovechará `pasoParalelo`: los dos tramos no comparten
partículas, así que se pueden calcular en hilos distintos al mismo tiempo.

---

### Ejemplo de uso

```scala
val tramo = pasoSegmento(tres, 2.0, 1, 3)
println(tramo.length)              // 2
println(tramo.map(_.pos).toList)   // List(Vector2(2.0,0.0), Vector2(3.0,7.0))
println(pasoSegmento(tres, 2.0, 1, 1).isEmpty)  // true
```

---

## 4. `pasoParalelo`

### Definición del algoritmo

```scala
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
```

* La función `pasoParalelo` avanza todo el sistema un intervalo `dt`, igual
  que `pasoSecuencial`, pero repartiendo el trabajo entre varios hilos.
* Recibe tres parámetros:

  * `s`: el sistema.
  * `dt`: el intervalo de tiempo.
  * `umbral`: el tamaño de tramo a partir del cual ya no vale la pena partir.
* `umb` es el umbral corregido: `math.max(umbral, 1)`. Vale lo mismo que
  `umbral`, salvo cuando este es 0 o negativo; entonces vale 1. Sin esa
  corrección, con umbral 0 un tramo de una partícula se partiría en uno vacío
  y en sí mismo, para siempre.
* `resolver(desde, hasta)` es una función auxiliar recursiva (divide y
  vencerás) que resuelve el tramo `[desde, hasta)`:

  * Si el tramo tiene `umb` partículas o menos, lo resuelve en secuencial con
    `pasoSegmento`.
  * Si no, lo parte por la mitad en `medio`, resuelve las dos mitades **al
    mismo tiempo** con `parallel` y pega los resultados con `++`.
* Si `s.length <= umb`, se llama a `pasoSecuencial(s, dt)`; en otro caso,
  la llamada inicial es `resolver(0, s.length)`: todo el sistema.

### Explicación paso a paso

1. Se calcula `umb`.
2. Si `n <= umb`, se ejecuta `pasoSecuencial(s, dt)`; en otro caso,
   `resolver(0, n)` mira el tamaño del tramo, `hasta - desde`.
3. Si es pequeño (`<= umb`), se llega a una **hoja** del árbol: `pasoSegmento`
   mueve esas partículas en el hilo actual.
4. Si es grande, se calcula `medio = desde + (hasta - desde) / 2` (división
   entera) y se crean dos subproblemas: `[desde, medio)` y `[medio, hasta)`.
5. `parallel` ejecuta el izquierdo en el hilo actual y entrega el derecho a
   otro hilo del `ForkJoinPool`; luego espera a los dos.
6. Con los dos resultados, `izq ++ der` construye el tramo completo, con la
   mitad izquierda primero, así que se conserva el orden.
7. Esto se repite en cada mitad hasta que todos los tramos son hojas.

---

### Evaluación con el modelo de sustitución

Ejemplo, con el sistema `tres` de la sección 2 y umbral 1:

```scala
pasoParalelo(tres, 2.0, 1)
```

#### Paso 1: Se calcula el umbral corregido y se hace la llamada inicial

```scala
umb = math.max(1, 1) = 1
if (s.length <= umb) pasoSecuencial(s, dt) else resolver(0, s.length)
→ resolver(0, s.length)      // 3 > 1: rama recursiva
→ resolver(0, tres.length)   // s = tres
→ resolver(0, 3)             // tres tiene 3 partículas: desde = 0, hasta = 3
```

El tramo inicial `[0, 3)` es todo el sistema. Los demás llamados a `resolver`
salen de partir este tramo en `medio`.

#### Paso 2: `resolver(0, 3)`, tramo de 3 > 1, se parte

```scala
medio = 0 + (3 - 0) / 2 = 1
parallel(resolver(0, 1), resolver(1, 3))
```

#### Paso 3: `resolver(0, 1)`, tramo de 1 ≤ 1, hoja

```scala
pasoSegmento(tres, 2.0, 0, 1)
→ Array(Particula(Vector2(2, 0), Vector2(1, 0), 1.0))
```

#### Paso 4: `resolver(1, 3)`, tramo de 2 > 1, se parte

```scala
medio = 1 + (3 - 1) / 2 = 2
parallel(resolver(1, 2), resolver(2, 3))
```

#### Paso 5: las dos hojas de `resolver(1, 3)`

```scala
resolver(1, 2) → pasoSegmento(tres, 2.0, 1, 2) → Array(Particula(Vector2(2, 0), Vector2(0, -1), 2.0))
resolver(2, 3) → pasoSegmento(tres, 2.0, 2, 3) → Array(Particula(Vector2(3, 7), Vector2(2, 2),  3.0))
```

#### Paso 6: se combinan los resultados de `resolver(1, 3)`

```scala
Array(pos (2, 0)) ++ Array(pos (3, 7))
→ Array(pos (2, 0), pos (3, 7))
```

#### Paso 7: se combinan los resultados de `resolver(0, 3)`

```scala
Array(pos (2, 0)) ++ Array(pos (2, 0), pos (3, 7))
→ Array(
    Particula(Vector2(2, 0), Vector2(1, 0),  1.0),
    Particula(Vector2(2, 0), Vector2(0, -1), 2.0),
    Particula(Vector2(3, 7), Vector2(2, 2),  3.0))
```

Es exactamente el resultado de `pasoSecuencial(tres, 2.0)` (sección 2).

#### Caso del umbral cero

```scala
pasoParalelo(Array(p), 1.0, 0)
→ umb = math.max(0, 1) = 1
→ pasoSecuencial(Array(p), 1.0)  // 1 <= umb: guarda inicial
→ Array(mover(p, 1.0))
```

Sin la corrección, `resolver(0, 1)` con umbral 0 calcularía `medio = 0` y se
llamaría a sí mismo como `resolver(0, 1)` sin fin.

---

### Árbol de llamados

Si se entra en `resolver`, cada nodo interno es un `parallel` y cada hoja
es un `pasoSegmento`. Para
`tres` con umbral 1:

```mermaid
graph TD
    A["resolver(0, 3): parallel"] --> B["resolver(0, 1): pasoSegmento"]
    A --> C["resolver(1, 3): parallel"]
    C --> D["resolver(1, 2): pasoSegmento"]
    C --> E["resolver(2, 3): pasoSegmento"]
```

Para un sistema de 8 partículas con umbral 2 el árbol queda balanceado, con 4
hojas de 2 partículas que pueden correr en 4 hilos al mismo tiempo:

```mermaid
graph TD
    A["0 a 8: parallel"] --> B["0 a 4: parallel"]
    A --> C["4 a 8: parallel"]
    B --> D["0 a 2: pasoSegmento"]
    B --> E["2 a 4: pasoSegmento"]
    C --> F["4 a 6: pasoSegmento"]
    C --> G["6 a 8: pasoSegmento"]
```

### Diagrama de hilos

`parallel` hace el lado izquierdo en el hilo actual y el derecho en otro, y
espera a los dos antes de concatenar:

```mermaid
sequenceDiagram
    participant H1 as Hilo 1: resolver(0, 3)
    participant H2 as Hilo 2: resolver(1, 3)
    participant H3 as Hilo 3: resolver(2, 3)

    H1->>H2: parallel: entrega [1, 3)
    H1->>H1: resolver(0, 1) → pasoSegmento
    H2->>H3: parallel: entrega [2, 3)
    H2->>H2: resolver(1, 2) → pasoSegmento
    H3-->>H2: Array(pos (3, 7))
    H2->>H2: Array(pos (2, 0)) ++ Array(pos (3, 7))
    H2-->>H1: Array(pos (2, 0), pos (3, 7))
    H1->>H1: Array(pos (2, 0)) ++ Array(pos (2, 0), pos (3, 7))
```

---

### Ejemplo de uso

```scala
val r1 = pasoParalelo(tres, 2.0, 1)    // parte hasta cada partícula
val r2 = pasoParalelo(tres, 2.0, 100)  // no parte: pasoSecuencial directo
val r3 = pasoParalelo(tres, 2.0, 0)    // umb = 1, igual que r1
println(r1.toList == pasoSecuencial(tres, 2.0).toList)  // true
println(r2.toList == r1.toList && r3.toList == r1.toList)  // true
```

---

## 5. `energiaSecuencial`

### Definición del algoritmo

```scala
def energiaSecuencial(s: Sistema): Double = {
  s.map(p => 0.5 * p.masa * p.vel.norma * p.vel.norma).sum
}
```

* La función `energiaSecuencial` calcula la energía cinética total del
  sistema.
* Recibe un parámetro, `s`: el sistema.
* La energía de una partícula es media masa por la rapidez al cuadrado:
  `0.5 * masa * rapidez * rapidez`. La rapidez es el tamaño del vector
  velocidad, `vel.norma`, que en `Modelo.scala` es `√(x² + y²)`.
* Usa `map` para calcular la energía de cada partícula y `sum` para sumarlas.
* No modifica `s`. Si `s` está vacío, el resultado es `0.0`.

### Explicación paso a paso

1. `s.map(p => ...)` recorre las partículas y, para cada una, calcula
   `0.5 * masa * norma * norma`. El resultado es un arreglo de números, uno
   por partícula.
2. `.sum` suma ese arreglo, empezando en `0.0`.

Multiplicar la norma por sí misma da `x² + y²`, así que solo importa el tamaño
de la velocidad, no su dirección: una partícula con velocidad `(3, 4)` y otra
con `(-3, -4)` tienen la misma energía.

---

### Evaluación con el modelo de sustitución

Ejemplo, con el sistema `tres` de la sección 2:

```scala
energiaSecuencial(tres)
```

#### Paso 1: Se reemplaza el cuerpo de la función

```scala
tres.map(p => 0.5 * p.masa * p.vel.norma * p.vel.norma).sum
```

#### Paso 2: Se calcula la norma de cada velocidad

```scala
Vector2(1, 0).norma   = √(1 + 0) = 1
Vector2(0, -1).norma  = √(0 + 1) = 1
Vector2(2, 2).norma   = √(4 + 4) = √8 ≈ 2.828
```

#### Paso 3: `map` calcula la energía de cada partícula

```scala
0.5 * 1.0 * 1 * 1     = 0.5
0.5 * 2.0 * 1 * 1     = 1.0
0.5 * 3.0 * √8 * √8   = 0.5 * 3.0 * 8 = 12.0
→ Array(0.5, 1.0, 12.0)
```

#### Paso 4: `sum` suma los resultados

```scala
Array(0.5, 1.0, 12.0).sum
→ 0.0 + 0.5 + 1.0 + 12.0
→ 13.5
```

Con `Double`, `√8 * √8` da `8.000000000000002` y no `8` exacto, así que el
resultado real puede diferir de `13.5` en la última cifra. Por eso las pruebas
comparan con `cerca`.

---

### Diagrama de llamados

```mermaid
sequenceDiagram
    participant E as energiaSecuencial(tres)
    participant Map as tres.map
    participant Sum as .sum

    E->>Map: energía de cada partícula
    Map-->>E: Array(0.5, 1.0, 12.0)
    E->>Sum: suma
    Sum-->>E: 13.5
```

---

### Ejemplo de uso

```scala
println(energiaSecuencial(tres))                      // 13.5 (aprox.)
println(energiaSecuencial(Array()))                   // 0.0
println(energiaSecuencial(pasoSecuencial(tres, 3.0))) // 13.5: moverse no cambia la velocidad
```

---

## 6. `masaTotal`

### Definición del algoritmo

```scala
def masaTotal(s: Sistema): Double = {
  s.map(p => p.masa).sum
}
```

* La función `masaTotal` suma las masas de todas las partículas.
* Recibe un parámetro, `s`: el sistema.
* `map` saca la masa de cada partícula y `sum` las suma.
* No modifica `s`. Si `s` está vacío, el resultado es `0.0`.

### Explicación paso a paso

1. `s.map(p => p.masa)` convierte el arreglo de partículas en un arreglo con
   sus masas, en el mismo orden.
2. `.sum` suma ese arreglo, empezando en `0.0`.

La posición y la velocidad no se usan, así que mover el sistema no cambia su
masa total.

---

### Evaluación con el modelo de sustitución

```scala
masaTotal(tres)
→ tres.map(p => p.masa).sum
→ Array(1.0, 2.0, 3.0).sum
→ 0.0 + 1.0 + 2.0 + 3.0
→ 6.0
```

---

### Diagrama de llamados

```mermaid
sequenceDiagram
    participant MT as masaTotal(tres)
    participant Map as tres.map
    participant Sum as .sum

    MT->>Map: masa de cada partícula
    Map-->>MT: Array(1.0, 2.0, 3.0)
    MT->>Sum: suma
    Sum-->>MT: 6.0
```

---

### Ejemplo de uso

```scala
println(masaTotal(tres))                       // 6.0
println(masaTotal(Array()))                    // 0.0
println(masaTotal(pasoSecuencial(tres, 5.0)))  // 6.0: moverse no cambia la masa
```

---

## 7. `energiaParalela` (punto 3)

### Definición del algoritmo

```scala
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
```

* Recibe el sistema `s` y el `umbral`, igual que `pasoParalelo`.
* Usa el mismo auxiliar local `resolver(desde, hasta)` y el mismo umbral
  corregido `umb`. Una hoja calcula la energía secuencial del recorte.
* En un nodo interno, `parallel` calcula las dos mitades y `izq + der` suma
  sus energías. Aquí se combinan números, no arreglos.
* El vacío es una hoja: `energiaSecuencial(Array())` da `0.0`.

### Evaluación con el modelo de sustitución

Con `tres` de la sección 2 y umbral 1, las energías son aproximadamente
$0{,}5$, $1$ y $12$:

```scala
energiaParalela(tres, 1)
→ umb = 1; resolver(0, 3)
→ parallel(resolver(0, 1), resolver(1, 3))
resolver(0, 1) → energiaSecuencial(tres.slice(0, 1)) → 0.5
resolver(1, 3) → parallel(resolver(1, 2), resolver(2, 3))
resolver(1, 2) → energiaSecuencial(tres.slice(1, 2)) → 1.0
resolver(2, 3) → energiaSecuencial(tres.slice(2, 3)) → 12.0 (aprox.)
resolver(1, 3) → 1.0 + 12.0 → 13.0 (aprox.)
resolver(0, 3) → 0.5 + 13.0 → 13.5 (aprox.)
```

### Árbol de llamados

```mermaid
graph TD
    A["resolver(0, 3): suma 13.5"] --> B["resolver(0, 1): hoja 0.5"]
    A --> C["resolver(1, 3): suma 13.0"]
    C --> D["resolver(1, 2): hoja 1.0"]
    C --> E["resolver(2, 3): hoja 12.0"]
```

Los valores se combinan después de que terminan las dos ramas. Los redondeos
de `Double` se comparan con `cerca`, como en la energía secuencial.

### Ejemplo de uso

```scala
println(energiaParalela(tres, 1))    // 13.5 (aprox.), hojas de una partícula
println(energiaParalela(tres, 10))   // 13.5 (aprox.), una sola hoja
println(energiaParalela(tres, 0))    // 13.5 (aprox.), umb = 1
```

---

## 8. `centroDeMasaParalelo` (punto 3)

### Definición del algoritmo

```scala
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
```

* Recibe `s` y `umbral`. El auxiliar devuelve un par: la suma de posiciones
  ponderadas y la masa del tramo. Todavía no devuelve un centro de masa.
* `foldLeft` empieza en `(Vector2(0, 0), 0.0)` y acumula una partícula a la
  vez: suma `p.pos * p.masa` al vector y `p.masa` a la masa.
* Los pares de las mitades se suman componente a componente. Solo después
  de resolver el sistema completo se multiplica el vector por `1.0 / masa`.
* Si la masa total es cero, devuelve el origen. No necesita llamar al centro
  secuencial del punto 2.

### Evaluación con el modelo de sustitución

Ejemplo con posiciones $(-4,2)$, $(2,-2)$ y $(6,4)$, masas $2$, $1$ y $3$,
y umbral 1. Las velocidades no intervienen:

```scala
resolver(0, 3) → parallel(resolver(0, 1), resolver(1, 3))
resolver(0, 1) → (Vector2(-8, 4), 2.0)
resolver(1, 3) → parallel(resolver(1, 2), resolver(2, 3))
resolver(1, 2) → (Vector2(2, -2), 1.0)
resolver(2, 3) → (Vector2(18, 12), 3.0)
resolver(1, 3) → (Vector2(2, -2) + Vector2(18, 12), 1.0 + 3.0)
              → (Vector2(20, 10), 4.0)
resolver(0, 3) → (Vector2(-8, 4) + Vector2(20, 10), 2.0 + 4.0)
              → (Vector2(12, 14), 6.0)
centroDeMasaParalelo(s, 1) → Vector2(12, 14) * (1.0 / 6.0)
                          → Vector2(2.0, 2.333333333333333) (aprox.)
```

El centro de la mitad izquierda sería $(-4,2)$ y el de la derecha $(5,2{,}5)$.
Promediarlos daría $(0{,}5,2{,}25)$, que no es $(2,7/3)$: las mitades pesan
$2$ y $4$, respectivamente.

### Árbol de llamados

```mermaid
graph TD
    A["0 a 3: suma (12,14), masa 6"] --> B["0 a 1: suma (-8,4), masa 2"]
    A --> C["1 a 3: suma (20,10), masa 4"]
    C --> D["1 a 2: suma (2,-2), masa 1"]
    C --> E["2 a 3: suma (18,12), masa 3"]
    A --> F["división final: centro (2,7/3)"]
```

### Caso sin masa

```scala
centroDeMasaParalelo(Array(), 0)
→ umb = 1; resolver(0, 0)
→ Array().foldLeft((Vector2(0, 0), 0.0))(...)
→ (Vector2(0, 0), 0.0)
→ Vector2(0, 0)
```

Una hoja con partículas de masa cero produce el mismo par neutro, aunque
sus posiciones estén lejos del origen. No se divide entre cero en las hojas.

---

## 9. `trabajo` (punto 4)

### Definición del algoritmo

```scala
def trabajo(n: Int): Int = {
  n
}
```

* Recibe el número de partículas, $n \geq 0$.
* El modelo cuenta una operación por partícula: $W(n) = n$. La función
  devuelve ese número directamente; no ejecuta la simulación.

### Evaluación con el modelo de sustitución

```scala
trabajo(13) → 13
trabajo(0) → 0
```

La evaluación no es recursiva. Calcular la estimación cuesta un número fijo
de pasos, aunque el trabajo que representa crezca con $n$.

---

## 10. `profundidad` (punto 4)

### Definición del algoritmo

```scala
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
```

* Recibe el tamaño $n \geq 0$ y el umbral. Corrige el umbral igual que P3.
* Una hoja cuenta su trabajo secuencial, `tamano`, no solamente un nivel.
* Un nodo interno cuenta una partición más la profundidad mayor de sus dos
  ramas, porque para combinar debe esperar a las dos.
* El corte usa división entera: con tamaño impar las ramas miden `medio` y
  `tamano - medio`. El elemento sobrante se conserva.
* El cálculo de la estimación se hace en secuencia; `math.max` describe el
  camino crítico del algoritmo modelado, no una llamada a `parallel`.

### Evaluación con el modelo de sustitución

Con 23 partículas y umbral 5:

```scala
profundidad(23, 5) → resolver(23)
resolver(23) → 1 + math.max(resolver(11), resolver(12))
resolver(11) → 1 + math.max(resolver(5), resolver(6))
resolver(5)  → 5
resolver(6)  → 1 + math.max(resolver(3), resolver(3)) → 1 + 3 → 4
resolver(11) → 1 + math.max(5, 4) → 6
resolver(12) → 1 + math.max(resolver(6), resolver(6)) → 1 + 4 → 5
resolver(23) → 1 + math.max(6, 5) → 7
```

La rama de 11 elementos cuesta más que la de 12: deja una hoja de 5 sin
partir. Por eso no basta con seguir siempre la mitad más grande.

### Árbol de llamados

Para `profundidad(9, 2)`:

```mermaid
graph TD
    A["9: 1 + max(3,4) = 5"] --> B["4: 1 + max(2,2) = 3"]
    A --> C["5: 1 + max(2,3) = 4"]
    B --> D["2: hoja, costo 2"]
    B --> E["2: hoja, costo 2"]
    C --> F["2: hoja, costo 2"]
    C --> G["3: 1 + max(1,2) = 3"]
    G --> H["1: hoja, costo 1"]
    G --> I["2: hoja, costo 2"]
```

### Ejemplo de uso

```scala
println(profundidad(1024, 1024))  // 1024: no parte
println(profundidad(1024, 512))   // 1 + 512 = 513
println(profundidad(1024, 1))     // 10 + 1 = 11
println(profundidad(0, 0))        // 0: hoja vacía
```

---

## 11. `tiempoEstimado` (punto 4)

### Definición del algoritmo

```scala
def tiempoEstimado(n: Int, p: Int, umbral: Int): Double = {
  profundidad(n, umbral) + trabajo(n).toDouble / p
}
```

* Recibe $n \geq 0$, el número de procesadores $p > 0$ y el umbral.
* Suma la profundidad y el trabajo dividido entre los procesadores.
  `toDouble` se aplica **antes** de dividir para conservar la fracción.
* El resultado está en unidades del modelo, no en milisegundos.

### Evaluación con el modelo de sustitución

```scala
tiempoEstimado(9, 4, 2)
→ profundidad(9, 2) + trabajo(9).toDouble / 4
→ 5 + 9.0 / 4
→ 5 + 2.25
→ 7.25
```

Con $p = 1$ la fórmula sigue sumando profundidad y trabajo; no se reemplaza
por el tiempo secuencial. Para el vacío y procesadores positivos da `0.0`.

---

## 12. `aceleracion` (punto 5)

### Definición del algoritmo

```scala
def aceleracion(tSecuencial: Double, tParalelo: Double): Double = {
  tSecuencial / tParalelo
}
```

Recibe dos tiempos en la misma unidad y devuelve su cociente. Para tiempos
positivos, un resultado mayor que uno indica mejora; menor que uno indica
ralentización. No convierte el cociente en porcentaje ni lo limita a uno.

### Evaluación con el modelo de sustitución

```scala
aceleracion(7.5, 3.0)
→ 7.5 / 3.0
→ 2.5

aceleracion(12.0, 48.0)
→ 12.0 / 48.0
→ 0.25
```

### Ejemplo de uso

En la medición real de un millón con umbral 500 000:
`aceleracion(64.076180, 50.740670)` da aproximadamente `1.262817`.
En `App` son medianas de cuatro muestras balanceadas de ScalaMeter,
no constantes fijadas en el programa. El control de un millón queda cerca
de uno, pero hay dispersión y el de 500 000 sigue sin validar: este cociente
no demuestra una mejora estable (véase `paralelizacion.md`).

---

## 13. `umbralConveniente` (punto 5)

### Definición del algoritmo

```scala
def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int = {
  if (umbrales.isEmpty) s.length
  else umbrales.map(u => (u, medir(3) { pasoParalelo(s, dt, u) })).minBy(_._2)._1
}
```

* Si no hay candidatos devuelve `s.length`, sin ejecutar mediciones.
* Si hay candidatos, `map` asocia cada umbral con la mediana de tres
  mediciones del paso completo. `minBy(_._2)` elige el menor tiempo y `_1`
  recupera su umbral. Se conserva el valor recibido, aunque P1 normalice
  internamente un umbral no positivo.
* Solo busca entre los candidatos recibidos. Un mínimo observado no asegura
  el mejor umbral universal ni el mismo ganador en otra ejecución.

### Evaluación con el modelo de sustitución

Para candidatos `List(a, b)`, sean `ta` y `tb` las medianas realmente medidas:

```scala
umbralConveniente(s, dt, List(a, b))
→ List((a, ta), (b, tb)).minBy(_._2)._1
→ a si ta <= tb; b si tb < ta

umbralConveniente(s, dt, Nil)
→ s.length
```

`ta` y `tb` son símbolos de la evaluación, no tiempos experimentales inventados.
En empate, `minBy` conserva el primero.

### Medición en `App` y ejemplo de uso

`umbralConveniente(s, 0.5, List(s.length))` devuelve ese único candidato.
`App` compara dos tamaños grandes, 500 000 y 1 000 000, y cuatro umbrales
por tamaño. Genera los datos y comprueba el resultado antes de cronometrar.
Imprime cada par medido, medianas en milisegundos, aceleración, MAD, rango de
cocientes, procesadores disponibles y mínimo observado. Usa cuatro rondas
balanceadas, tres precalentamientos por ruta y candidato, `Warmer.Default`
con 5..10 calentamientos y `benchRuns = 5`; mide el paso completo y entrega
su longitud al medidor. No altera el `medir` proporcionado.
El experimento y sus límites están en `paralelizacion.md`.
