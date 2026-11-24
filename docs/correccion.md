# Informe de corrección

## 1. `mover`

### Especificación

Una partícula es una tripleta $(\vec{p}, \vec{v}, m)$ con posición
$\vec{p} = (p_x, p_y)$, velocidad $\vec{v} = (v_x, v_y)$ y masa $m$.

Sea $f$ la función que, dado un intervalo de tiempo $dt$, devuelve la partícula
después de moverse con velocidad constante. La posición cambia y la velocidad
y la masa se conservan:

```math
f((\vec{p}, \vec{v}, m), dt) = (\vec{p} + dt \cdot \vec{v},\ \vec{v},\ m)
```

Componente a componente:

```math
f(((p_x, p_y), (v_x, v_y), m), dt) = ((p_x + v_x \cdot dt,\ p_y + v_y \cdot dt),\ (v_x, v_y),\ m)
```

### Programa

Sea $P_f$ el programa en Scala:

```scala
def mover(p: Particula, dt: Double): Particula = {
  Particula(p.pos + p.vel * dt, p.vel, p.masa)
}
```

Hay que demostrar:

```math
\forall p, \forall dt : \text{mover}(p, dt) == f(p, dt)
```

### Demostración

`mover` **no es recursiva**: su cuerpo es una sola expresión, sin llamados a sí
misma. Por eso no hace falta inducción; basta con evaluar el programa con el
**modelo de sustitución** para una partícula cualquiera y comparar con la
especificación.

Sea $p = \text{Particula}(\text{Vector2}(p_x, p_y), \text{Vector2}(v_x, v_y), m)$
una partícula cualquiera y $dt$ un real cualquiera.

**Paso 1.** Se reemplaza el cuerpo de `mover`:

```math
\text{mover}(p, dt) \rightarrow \text{Particula}(\text{Vector2}(p_x, p_y) + \text{Vector2}(v_x, v_y) \ast dt,\ \text{Vector2}(v_x, v_y),\ m)
```

**Paso 2.** Se evalúa `*` con su definición en `Vector2`,
$\text{Vector2}(x, y) \ast k = \text{Vector2}(x \cdot k, y \cdot k)$:

```math
\rightarrow \text{Particula}(\text{Vector2}(p_x, p_y) + \text{Vector2}(v_x \cdot dt,\ v_y \cdot dt),\ \text{Vector2}(v_x, v_y),\ m)
```

**Paso 3.** Se evalúa `+` con su definición en `Vector2`,
$\text{Vector2}(a, b) + \text{Vector2}(c, d) = \text{Vector2}(a + c, b + d)$:

```math
\rightarrow \text{Particula}(\text{Vector2}(p_x + v_x \cdot dt,\ p_y + v_y \cdot dt),\ \text{Vector2}(v_x, v_y),\ m)
```

**Paso 4.** Se compara con la especificación:

```math
f(p, dt) = ((p_x + v_x \cdot dt,\ p_y + v_y \cdot dt),\ (v_x, v_y),\ m)
```

Los tres campos coinciden:

| Campo | `mover(p, dt)` | $f(p, dt)$ |
|---|---|---|
| posición | $(p_x + v_x \cdot dt,\ p_y + v_y \cdot dt)$ | $(p_x + v_x \cdot dt,\ p_y + v_y \cdot dt)$ |
| velocidad | $(v_x, v_y)$ | $(v_x, v_y)$ |
| masa | $m$ | $m$ |

Como $p$ y $dt$ eran arbitrarios, concluimos:

```math
\forall p, \forall dt : \text{mover}(p, dt) == f(p, dt)
```

### Terminación

La evaluación de `mover` hace siempre el mismo número de pasos: una
multiplicación de vector por escalar, una suma de vectores y la construcción de
una `Particula`. Ninguno de esos pasos es recursivo, así que `mover` termina
para cualquier entrada.

### Ausencia de efectos

`mover` no modifica `p`: `Particula` y `Vector2` son `case class` inmutables y
el programa solo construye valores nuevos. Esto garantiza que llamar
`mover` desde varios hilos al mismo tiempo (como hará `pasoParalelo`) no puede
producir resultados distintos a llamarla en secuencia.

### Comprobación con las pruebas

| Prueba | Entrada | Esperado |
|---|---|---|
| velocidad negativa | pos $(5,5)$, vel $(-2,-1)$, $dt = 2$ | pos $(1, 3)$ |
| $dt = 0$ | pos $(7,-3)$, vel $(10,10)$, $dt = 0$ | pos $(7, -3)$ |
| velocidad y masa se conservan | vel $(3,4)$, $m = 5$, $dt = 3$ | vel $(3,4)$, $m = 5$ |
| $dt$ decimal | pos $(1,1)$, vel $(4,-2)$, $dt = 0.5$ | pos $(3, 0)$ |
| dos pasos de 1 = un paso de 2 | pos $(0,0)$, vel $(3,1)$ | pos $(6, 2)$ en ambos casos |

La última prueba refleja una propiedad que se sigue de la especificación:

```math
\vec{p} + dt_1 \cdot \vec{v} + dt_2 \cdot \vec{v} = \vec{p} + (dt_1 + dt_2) \cdot \vec{v}
```

---

## 2. `pasoSecuencial`

### Especificación

Un sistema es una secuencia de partículas $s = (a_0, a_1, \ldots, a_{n-1})$,
con $n \geq 0$. Sea $g$ la función que avanza todo el sistema un intervalo
$dt$: cada partícula se mueve según la especificación $f$ de `mover`, y el
sistema conserva su tamaño y su orden.

```math
g((a_0, a_1, \ldots, a_{n-1}), dt) = (f(a_0, dt),\ f(a_1, dt),\ \ldots,\ f(a_{n-1}, dt))
```

### Programa

```scala
def pasoSecuencial(s: Sistema, dt: Double): Sistema = {
  s.map(p => mover(p, dt))
}
```

Hay que demostrar:

```math
\forall n \in \mathbb{N},\ \forall dt : \text{pasoSecuencial}((a_0, \ldots, a_{n-1}), dt) == g((a_0, \ldots, a_{n-1}), dt)
```

### Comportamiento de `map`

Para razonar por inducción usamos cómo se comporta `map` sobre una secuencia,
según su tamaño:

```math
() .\text{map}(h) = ()
```

```math
(a_0, a_1, \ldots, a_k).\text{map}(h) = h(a_0) \,::\, (a_1, \ldots, a_k).\text{map}(h)
```

donde $x :: (\ldots)$ pone $x$ al inicio de la secuencia.

### Demostración por inducción sobre $n$, el tamaño del sistema

Sea $h = (p \Rightarrow \text{mover}(p, dt))$. Por la sección 1 sabemos que
$\text{mover}(p, dt) == f(p, dt)$ para toda partícula $p$, así que
$h(p) == f(p, dt)$.

**Caso base:** $n = 0$, el sistema vacío.

```math
\text{pasoSecuencial}((), dt) \rightarrow ().\text{map}(h) \rightarrow ()
```

Por otro lado, $g((), dt) = ()$. Entonces
$\text{pasoSecuencial}((), dt) == g((), dt)$.

**Caso de inducción:** $n = k + 1$, $k \geq 0$. Hipótesis de inducción (HI):

```math
\text{pasoSecuencial}((a_1, \ldots, a_k), dt) == (f(a_1, dt), \ldots, f(a_k, dt))
```

Hay que demostrar que se cumple para $(a_0, a_1, \ldots, a_k)$:

```math
\text{pasoSecuencial}((a_0, \ldots, a_k), dt) \rightarrow (a_0, \ldots, a_k).\text{map}(h)
```

```math
\rightarrow h(a_0) \,::\, (a_1, \ldots, a_k).\text{map}(h)
```

```math
\rightarrow h(a_0) \,::\, \text{pasoSecuencial}((a_1, \ldots, a_k), dt)
```

Usando la sección 1 en la cabeza y la HI en la cola:

```math
\rightarrow f(a_0, dt) \,::\, (f(a_1, dt), \ldots, f(a_k, dt)) = (f(a_0, dt), f(a_1, dt), \ldots, f(a_k, dt))
```

```math
= g((a_0, a_1, \ldots, a_k), dt)
```

Concluimos por inducción que:

```math
\forall n \in \mathbb{N} : \text{pasoSecuencial}((a_0, \ldots, a_{n-1}), dt) == g((a_0, \ldots, a_{n-1}), dt)
```

### Consecuencias de la especificación

De la demostración salen directamente las tres propiedades que revisan las
pruebas:

- **Tamaño:** el resultado tiene $n$ elementos, uno por cada partícula.
- **Orden:** el elemento $i$ del resultado es $f(a_i, dt)$, viene de la
  partícula $i$.
- **Sin efectos:** `map` construye un arreglo nuevo y `mover` no modifica
  sus argumentos, así que $s$ queda igual.

### Terminación

Cada partícula se procesa una sola vez y el tamaño $n$ es finito. En la
inducción, cada paso reduce el tamaño en 1 hasta llegar a $0$. Por lo tanto,
`pasoSecuencial` termina después de $n$ llamados a `mover`.

### Comprobación con las pruebas

| Prueba | Entrada | Esperado |
|---|---|---|
| cada una con su velocidad | `tres`, $dt = 2$ | posiciones $(2,0)$, $(2,0)$, $(3,7)$ |
| conserva el tamaño | `tres`, $dt = 1$ | 3 partículas |
| conserva el orden | `tres`, $dt = 1$ | masas $1, 2, 3$ |
| no modifica el original | `tres`, $dt = 5$ | `tres` sigue en $(0,0)$, …, $(-1,3)$ |
| partículas quietas | velocidades $(0,0)$, $dt = 10$ | el mismo sistema |

---

## 3. `pasoSegmento`

### Especificación

Sea $s = (a_0, a_1, \ldots, a_{n-1})$ un sistema y sean `desde` $= d$ y
`hasta` $= k$ dos índices con $0 \leq d \leq k \leq n$. Sea $h$ la función que
avanza un intervalo $dt$ **solo** las partículas del tramo $[d, k)$, es decir,
desde la posición $d$ incluida hasta la $k$ excluida, y devuelve únicamente ese
tramo:

```math
h((a_0, \ldots, a_{n-1}), dt, d, k) = (f(a_d, dt),\ f(a_{d+1}, dt),\ \ldots,\ f(a_{k-1}, dt))
```

El resultado tiene $k - d$ elementos. Si $d = k$ el tramo es vacío y el
resultado es $()$.

### Programa

```scala
def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
  s.slice(desde, hasta).map(p => mover(p, dt))
}
```

Hay que demostrar:

```math
\forall s, \forall dt, \forall d, k \text{ con } 0 \leq d \leq k \leq n : \text{pasoSegmento}(s, dt, d, k) == h(s, dt, d, k)
```

### Comportamiento de `slice`

Para $0 \leq d \leq k \leq n$, `slice` devuelve un arreglo nuevo con los
elementos entre $d$ incluido y $k$ excluido, en el mismo orden:

```math
(a_0, \ldots, a_{n-1}).\text{slice}(d, k) = (a_d, a_{d+1}, \ldots, a_{k-1})
```

y si $d = k$, el resultado es $()$. `slice` no modifica el arreglo original.

### Demostración

`pasoSegmento` **no es recursiva**: es la composición de `slice` con el mismo
`map` de `pasoSecuencial`. Basta con evaluar con el modelo de sustitución y
reutilizar el resultado de la sección 2.

Sea $s = (a_0, \ldots, a_{n-1})$, $dt$ cualquiera y $0 \leq d \leq k \leq n$.

**Paso 1.** Se reemplaza el cuerpo de `pasoSegmento`:

```math
\text{pasoSegmento}(s, dt, d, k) \rightarrow s.\text{slice}(d, k).\text{map}(p \Rightarrow \text{mover}(p, dt))
```

**Paso 2.** Se evalúa `slice`:

```math
\rightarrow (a_d, \ldots, a_{k-1}).\text{map}(p \Rightarrow \text{mover}(p, dt))
```

**Paso 3.** La expresión que queda es exactamente el cuerpo de
`pasoSecuencial` aplicado al sistema $(a_d, \ldots, a_{k-1})$, que tiene
$k - d$ elementos:

```math
\rightarrow \text{pasoSecuencial}((a_d, \ldots, a_{k-1}), dt)
```

**Paso 4.** En la sección 2 se demostró, por inducción sobre el tamaño, que
para **cualquier** sistema finito
$\text{pasoSecuencial}((b_0, \ldots, b_{m-1}), dt) == (f(b_0, dt), \ldots, f(b_{m-1}, dt))$.
Tomando $b_i = a_{d+i}$ y $m = k - d$:

```math
\rightarrow (f(a_d, dt), \ldots, f(a_{k-1}, dt)) = h(s, dt, d, k)
```

En el caso $d = k$, el paso 2 da $()$ y el caso base de la sección 2 da
$().\text{map}(\ldots) = ()$, que coincide con $h(s, dt, d, d) = ()$.

Como $s$, $dt$, $d$ y $k$ eran arbitrarios, concluimos:

```math
\forall s, \forall dt, \forall\, 0 \leq d \leq k \leq n : \text{pasoSegmento}(s, dt, d, k) == h(s, dt, d, k)
```

### Lema: partir un tramo no pierde ni repite partículas

Este es el resultado que necesita `pasoParalelo`. Para cualquier punto de corte
$m$ con $d \leq m \leq k$:

```math
\text{pasoSegmento}(s, dt, d, m) \mathbin{+\!\!+} \text{pasoSegmento}(s, dt, m, k) == \text{pasoSegmento}(s, dt, d, k)
```

**Demostración.** Por el teorema anterior:

```math
\text{pasoSegmento}(s, dt, d, m) \mathbin{+\!\!+} \text{pasoSegmento}(s, dt, m, k)
```

```math
= (f(a_d, dt), \ldots, f(a_{m-1}, dt)) \mathbin{+\!\!+} (f(a_m, dt), \ldots, f(a_{k-1}, dt))
```

```math
= (f(a_d, dt), \ldots, f(a_{m-1}, dt), f(a_m, dt), \ldots, f(a_{k-1}, dt))
```

```math
= h(s, dt, d, k) = \text{pasoSegmento}(s, dt, d, k)
```

Los índices $[d, m)$ y $[m, k)$ son disjuntos (ninguna partícula se repite) y
su unión es $[d, k)$ (ninguna se pierde). Además
$(m - d) + (k - m) = k - d$, así que el tamaño se conserva. En particular,
con $d = 0$ y $k = n$:

```math
\text{pasoSegmento}(s, dt, 0, m) \mathbin{+\!\!+} \text{pasoSegmento}(s, dt, m, n) == \text{pasoSecuencial}(s, dt)
```

### Índices fuera de rango

La especificación solo pide $0 \leq d \leq k \leq n$, que es el único caso en
que la llamará `pasoParalelo`. Aun así, `slice` no lanza excepciones con
índices fuera de rango: recorta $d$ y $k$ al intervalo $[0, n]$ y, si
$k \leq d$, devuelve $()$. Por eso `pasoSegmento` es una función **total**:
siempre devuelve un sistema, nunca falla.

### Terminación

`slice` copia $k - d$ elementos y `map` llama a `mover` $k - d$ veces; `mover`
termina (sección 1). Ningún paso es recursivo, así que `pasoSegmento` termina
después de $k - d$ llamados a `mover`.

### Ausencia de efectos

`slice` y `map` construyen arreglos nuevos y `mover` no modifica sus
argumentos, así que $s$ queda igual. Como cada llamado solo **lee** las
posiciones $[d, k)$ de $s$ y escribe en un arreglo propio, dos llamados con
tramos distintos pueden ejecutarse al mismo tiempo en hilos distintos sin
interferir, que es lo que hará `pasoParalelo`.

### Comprobación con las pruebas

Con `s` = partícula en $(0,0)$ con velocidad $(3,4)$, y partícula en $(4,0)$
con velocidad $(0,0)$:

| Prueba | Entrada | Esperado |
|---|---|---|
| solo calcula el tramo pedido | `pasoSegmento(s, 1.0, 1, 2)` | 1 partícula, en $(4, 0)$ |
| el tramo vacío da un sistema vacío | `pasoSegmento(s, 1.0, 1, 1)` | $()$ |

En la primera, $h(s, 1, 1, 2) = (f(a_1, 1)) = ((4,0) + 1 \cdot (0,0)) = ((4, 0))$:
la partícula $a_0$ no aparece en el resultado. En la segunda, $d = k = 1$ y
$h = ()$.

Pruebas propias, con el sistema `tres` de la sección 2 y el sistema `grande`
de 1000 partículas:

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| tramo intermedio | `pasoSegmento(tres, 2.0, 1, 3)` | 2 partículas, en $(2,0)$ y $(3,7)$ | la especificación $h$ |
| tramo completo | `pasoSegmento(tres, 2.0, 0, 3)` | igual a `pasoSecuencial(tres, 2.0)` | caso $d = 0$, $k = n$ |
| dos tramos unidos | cortes $m = 0, 1, 2, 3$, $dt = 1.5$ | `tramo(0, m) ++ tramo(m, 3)` igual al paso completo | el lema: no se pierde ni se repite nada |
| tamaño | `pasoSegmento(grande, 0.5, 100, 350)` | 250 partículas | el resultado tiene $k - d$ elementos |
| sin efectos | `pasoSegmento(tres, 5.0, 0, 3)` | `tres` sigue en $(0,0)$, $(2,2)$, $(-1,3)$ | `s` no se modifica |

---

## 4. `pasoParalelo`

### Especificación

`pasoParalelo` debe calcular **lo mismo** que `pasoSecuencial`, sin importar
el umbral; lo único que cambia es cómo se reparte el trabajo. Con
$s = (a_0, \ldots, a_{n-1})$ y cualquier entero $\text{umbral}$:

```math
\text{pasoParalelo}(s, dt, \text{umbral}) == g(s, dt) = (f(a_0, dt), \ldots, f(a_{n-1}, dt))
```

### Programa

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
  resolver(0, s.length)
}
```

Hay que demostrar:

```math
\forall s, \forall dt, \forall \text{umbral} \in \mathbb{Z} : \text{pasoParalelo}(s, dt, \text{umbral}) == g(s, dt)
```

### Comportamiento de `parallel`

`parallel(a, b)` evalúa `a` en el hilo actual y `b` en otra tarea del
`ForkJoinPool`, espera a las dos y devuelve el par `(a, b)`. Si ninguna de las
dos expresiones tiene efectos, el resultado es el mismo que evaluarlas en
secuencia:

```math
\text{parallel}(a, b) == (a, b)
```

`resolver` no tiene efectos: solo lee $s$ (que nadie modifica) y construye
arreglos nuevos (secciones 1 y 3). Por eso, para razonar sobre el **valor** de
`pasoParalelo` se puede reemplazar `parallel` por el par, y el orden en que
terminen los hilos no importa.

### El umbral corregido $\text{umb}$

Sea $\text{umb} = \max(\text{umbral}, 1)$. Entonces $\text{umb} \geq 1$ para cualquier umbral,
incluso $0$ o negativo. Esto es lo que garantiza la terminación (ver abajo).

### Lema: `resolver` calcula el segmento

```math
\forall d, k \text{ con } 0 \leq d \leq k \leq n : \text{resolver}(d, k) == \text{pasoSegmento}(s, dt, d, k)
```

**Demostración por inducción fuerte sobre $L = k - d$**, el tamaño del tramo.
Hipótesis de inducción (HI): el lema vale para todo tramo de tamaño menor que
$L$.

**Caso base: $L \leq \text{umb}$.** La condición del `if` es verdadera:

```math
\text{resolver}(d, k) \rightarrow \text{pasoSegmento}(s, dt, d, k)
```

Esto incluye el tramo vacío, $L = 0$.

**Caso de inducción: $L > \text{umb}$.** Como $\text{umb} \geq 1$, se tiene $L \geq 2$. Sea

```math
m = d + \left\lfloor \tfrac{L}{2} \right\rfloor
```

Como $L \geq 2$, $1 \leq \lfloor L/2 \rfloor \leq L - 1$, así que
$d < m < k$. Los dos subtramos tienen tamaños $m - d \geq 1$ y $k - m \geq 1$,
ambos **estrictamente menores** que $L$. Evaluando:

```math
\text{resolver}(d, k) \rightarrow \text{izq} \mathbin{+\!\!+} \text{der}
\quad\text{con}\quad (\text{izq}, \text{der}) = \text{parallel}(\text{resolver}(d, m), \text{resolver}(m, k))
```

Por el comportamiento de `parallel`:

```math
\rightarrow \text{resolver}(d, m) \mathbin{+\!\!+} \text{resolver}(m, k)
```

Por la HI aplicada a los dos subtramos:

```math
\rightarrow \text{pasoSegmento}(s, dt, d, m) \mathbin{+\!\!+} \text{pasoSegmento}(s, dt, m, k)
```

Y por el lema de la sección 3 (partir un tramo no pierde ni repite
partículas), con $d \leq m \leq k$:

```math
= \text{pasoSegmento}(s, dt, d, k)
```

Lo que concluye el caso de inducción.

### Teorema

`pasoParalelo` llama a $\text{resolver}(0, n)$. Por el lema, con $d = 0$ y
$k = n$, y por la sección 3:

```math
\text{pasoParalelo}(s, dt, \text{umbral}) \rightarrow \text{resolver}(0, n) == \text{pasoSegmento}(s, dt, 0, n) == h(s, dt, 0, n) = g(s, dt)
```

Como $s$, $dt$ y $\text{umbral}$ eran arbitrarios:

```math
\forall s, \forall dt, \forall \text{umbral} : \text{pasoParalelo}(s, dt, \text{umbral}) == \text{pasoSecuencial}(s, dt)
```

### La combinación `++` es asociativa

Según el umbral, el árbol de llamados agrupa las concatenaciones de formas
distintas. Por ejemplo, con 3 tramos $x, y, z$ puede quedar
$(x \mathbin{+\!\!+} y) \mathbin{+\!\!+} z$ o
$x \mathbin{+\!\!+} (y \mathbin{+\!\!+} z)$. Que todos los umbrales den el
mismo resultado se apoya en que `++` es asociativa:

```math
\forall x, y, z : (x \mathbin{+\!\!+} y) \mathbin{+\!\!+} z == x \mathbin{+\!\!+} (y \mathbin{+\!\!+} z)
```

con $()$ como elemento neutro. Se usa cómo se comporta `++`:

```math
() \mathbin{+\!\!+} y = y \qquad (a :: x') \mathbin{+\!\!+} y = a :: (x' \mathbin{+\!\!+} y)
```

**Demostración por inducción sobre $x$.**

*Caso base*, $x = ()$:

```math
(() \mathbin{+\!\!+} y) \mathbin{+\!\!+} z = y \mathbin{+\!\!+} z = () \mathbin{+\!\!+} (y \mathbin{+\!\!+} z)
```

*Caso de inducción*, $x = a :: x'$, con HI
$(x' \mathbin{+\!\!+} y) \mathbin{+\!\!+} z == x' \mathbin{+\!\!+} (y \mathbin{+\!\!+} z)$:

```math
((a :: x') \mathbin{+\!\!+} y) \mathbin{+\!\!+} z
= (a :: (x' \mathbin{+\!\!+} y)) \mathbin{+\!\!+} z
= a :: ((x' \mathbin{+\!\!+} y) \mathbin{+\!\!+} z)
```

```math
\overset{HI}{=} a :: (x' \mathbin{+\!\!+} (y \mathbin{+\!\!+} z))
= (a :: x') \mathbin{+\!\!+} (y \mathbin{+\!\!+} z)
```

Nótese que `++` **no** es conmutativa: $x \mathbin{+\!\!+} y \neq y \mathbin{+\!\!+} x$
en general. No hace falta que lo sea, porque `parallel` devuelve el par en el
orden en que se le pasaron las expresiones (`izq` primero, `der` después),
aunque `der` termine antes. Por eso se conserva el orden de las partículas.

### Terminación

En cada llamado recursivo el tamaño $L = k - d$ baja estrictamente (caso de
inducción: los subtramos miden entre $1$ y $L - 1$), y $L$ es un natural. Una
sucesión estrictamente decreciente de naturales es finita, así que toda rama
del árbol llega a un tramo con $L \leq \text{umb}$ y termina en `pasoSegmento`, que
termina (sección 3).

Aquí es donde hace falta $\text{umb} \geq 1$. Si se usara `umbral` directamente con
$\text{umbral} = 0$, un tramo de tamaño $L = 1$ no cumple $1 \leq 0$, se
parte en $m = d + \lfloor 1/2 \rfloor = d$ y produce
$\text{resolver}(d, d+1)$, **el mismo llamado**: $L$ no baja y la recursión no
termina (`StackOverflowError`). Con $\text{umb} = \max(\text{umbral}, 1)$, todo tramo
de tamaño $1$ es caso base.

### Ausencia de efectos y ausencia de carreras

Los hilos solo **leen** $s$ y cada uno escribe en arreglos nuevos propios (los
que crean `slice`, `map` y `++`). Dos hilos nunca escriben en el mismo arreglo
y $s$ no se modifica, así que no hay condiciones de carrera y el resultado no
depende de cómo el `ForkJoinPool` reparta las tareas.

### Comprobación con las pruebas

Pruebas dadas:

| Prueba | Entrada | Esperado |
|---|---|---|
| caso simple | `pasoParalelo(Array(p), 2.0, 1)` | pos $(2, 4)$ |
| varios umbrales | `grande`, umbrales $1, 2, 7, 64, 1000, 5000$ | igual al secuencial |
| orden y tamaño | `grande`, umbral $32$ | $1000$ partículas |
| umbral cero | `Array(p)`, umbral $0$ | $1$ partícula, sin desbordar la pila |

Pruebas propias:

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| umbral 1 | `pasoParalelo(tres, 2.0, 1)` | igual a `pasoSecuencial(tres, 2.0)` | el árbol más profundo: cada hoja es una partícula |
| umbral mayor que $n$ | `pasoParalelo(tres, 2.0, 100)` | igual al secuencial | caso base directo, sin partir |
| sistema vacío | `pasoParalelo(vacio, 1.0, 4)` | $()$ | caso $L = 0$ |
| umbral negativo | `pasoParalelo(tres, 1.0, -5)` | igual al secuencial | $\text{umb} = \max(-5, 1) = 1$, termina |
| umbral impar | `pasoParalelo(grande, 0.5, 3)` | masas en el mismo orden que `grande` | `++` conserva el orden aunque los tramos sean desiguales |
