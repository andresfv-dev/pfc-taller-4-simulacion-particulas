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
  if (s.length <= umb) pasoSecuencial(s, dt) else resolver(0, s.length)
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

Si $n \leq \text{umb}$, llama a `pasoSecuencial(s, dt)`, que calcula $g(s, dt)$
por la sección 2. Esta guarda mínima añadida para el control de P5 evita
copiar todo el arreglo cuando no se divide. Si $n > \text{umb}$,
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

---

## 5. `energiaSecuencial`

### Especificación

La energía cinética de una partícula con masa $m$ y velocidad
$\vec{v} = (v_x, v_y)$ es media masa por la rapidez al cuadrado:

```math
e(a) = \tfrac{1}{2}\, m\, |\vec{v}|^2 = \tfrac{1}{2}\, m\, (v_x^2 + v_y^2)
```

La energía del sistema $s = (a_0, \ldots, a_{n-1})$ es la suma de las
energías de sus partículas:

```math
E(s) = \sum_{i=0}^{n-1} e(a_i)
```

Para el sistema vacío la suma no tiene términos y vale $0$.

### Programa

```scala
def energiaSecuencial(s: Sistema): Double = {
  s.map(p => 0.5 * p.masa * p.vel.norma * p.vel.norma).sum
}
```

Hay que demostrar:

```math
\forall s : \text{energiaSecuencial}(s) == E(s)
```

### Comportamiento de `norma` y de `sum`

`norma` está definida en `Vector2` como $\sqrt{x^2 + y^2}$. Como
$x^2 + y^2 \geq 0$:

```math
\text{norma}(\vec{v}) \cdot \text{norma}(\vec{v}) = \left(\sqrt{v_x^2 + v_y^2}\right)^2 = v_x^2 + v_y^2 = |\vec{v}|^2
```

Así que la lambda calcula justo la energía de una partícula:

```math
0.5 \cdot m \cdot \text{norma}(\vec{v}) \cdot \text{norma}(\vec{v}) = \tfrac{1}{2}\, m\, |\vec{v}|^2 = e(a)
```

`sum` suma los elementos de una secuencia empezando en $0$:

```math
().\text{sum} = 0 \qquad (x_0, x_1, \ldots, x_k).\text{sum} = x_0 + (x_1, \ldots, x_k).\text{sum}
```

### Demostración por inducción sobre $n$, el tamaño del sistema

**Caso base:** $n = 0$.

```math
\text{energiaSecuencial}(()) \rightarrow ().\text{map}(\ldots).\text{sum} \rightarrow ().\text{sum} \rightarrow 0 = E(())
```

**Caso de inducción:** $n = k + 1$. Hipótesis de inducción (HI):

```math
\text{energiaSecuencial}((a_1, \ldots, a_k)) == \sum_{i=1}^{k} e(a_i)
```

Entonces:

```math
\text{energiaSecuencial}((a_0, \ldots, a_k)) \rightarrow (a_0, \ldots, a_k).\text{map}(\ldots).\text{sum}
```

```math
\rightarrow (e(a_0), e(a_1), \ldots, e(a_k)).\text{sum}
```

```math
\rightarrow e(a_0) + (e(a_1), \ldots, e(a_k)).\text{sum}
```

```math
\overset{HI}{=} e(a_0) + \sum_{i=1}^{k} e(a_i) = \sum_{i=0}^{k} e(a_i) = E((a_0, \ldots, a_k))
```

En el segundo paso se usa que `map` aplica la lambda a cada partícula y que
la lambda calcula $e(a_i)$ (ver arriba). Concluimos que
$\forall s : \text{energiaSecuencial}(s) == E(s)$.

### Sobre los números reales y los `Double`

La demostración es con números reales. Con `Double` hay redondeo: por ejemplo,
$\sqrt{8} \cdot \sqrt{8}$ da $8{,}000000000000002$ y no $8$ exacto. Por eso las
pruebas comparan con `cerca` (diferencia menor que $10^{-6}$) y no con `==`.

### Terminación

`map` recorre las $n$ partículas una vez y `sum` recorre los $n$ resultados
una vez. Ningún paso es recursivo, así que termina después de $2n$ pasos.

### Ausencia de efectos

`map` construye un arreglo nuevo y la lambda solo lee la partícula, así que
$s$ no se modifica.

### Comprobación con las pruebas

Pruebas dadas:

| Prueba | Entrada | Esperado |
|---|---|---|
| media masa por velocidad al cuadrado | `s`: masa 2, velocidad $(3,4)$; masa 2, quieta | $\tfrac12 \cdot 2 \cdot 25 + 0 = 25$ |
| sistema vacío | `vacio` | $0$ |
| partícula quieta | `Array(s(1))` | $0$ |

Pruebas propias:

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| suma de cada partícula | `tres` | $0{,}5 + 1 + 12 = 13{,}5$ | la suma de la especificación |
| dirección no importa | velocidad $(-3,-4)$, masa 2 | $25$ | $|\vec v|^2$ no depende del signo |
| doble de masa | velocidad $(1,2)$, masas 1 y 2 | el doble | $e$ es proporcional a $m$ |
| doble de velocidad | velocidades $(1,2)$ y $(2,4)$ | cuatro veces | $e$ crece con el cuadrado de $|\vec v|$ |
| un paso no cambia la energía | `pasoSecuencial(tres, 3.0)` | $13{,}5$ | `mover` no cambia velocidad ni masa |

---

## 6. `masaTotal`

### Especificación

La masa total del sistema $s = (a_0, \ldots, a_{n-1})$, con $m_i$ la masa de
$a_i$, es la suma de las masas:

```math
M(s) = \sum_{i=0}^{n-1} m_i
```

Para el sistema vacío vale $0$.

### Programa

```scala
def masaTotal(s: Sistema): Double = {
  s.map(p => p.masa).sum
}
```

Hay que demostrar:

```math
\forall s : \text{masaTotal}(s) == M(s)
```

### Demostración por inducción sobre $n$

Es la misma forma que la sección 5, cambiando $e(a_i)$ por $m_i$.

**Caso base:** $n = 0$.

```math
\text{masaTotal}(()) \rightarrow ().\text{map}(p \Rightarrow p.\text{masa}).\text{sum} \rightarrow ().\text{sum} \rightarrow 0 = M(())
```

**Caso de inducción:** $n = k + 1$, con HI
$\text{masaTotal}((a_1, \ldots, a_k)) == \sum_{i=1}^{k} m_i$:

```math
\text{masaTotal}((a_0, \ldots, a_k)) \rightarrow (m_0, m_1, \ldots, m_k).\text{sum}
\rightarrow m_0 + (m_1, \ldots, m_k).\text{sum}
```

```math
\overset{HI}{=} m_0 + \sum_{i=1}^{k} m_i = \sum_{i=0}^{k} m_i = M((a_0, \ldots, a_k))
```

Concluimos que $\forall s : \text{masaTotal}(s) == M(s)$.

### Consecuencia: la masa de las partes suma la del todo

Si se parte un sistema en dos tramos $x$ y $y$, la suma de todas las masas es
la suma de las masas de cada tramo:

```math
M(x \mathbin{+\!\!+} y) = M(x) + M(y)
```

Esto es lo que permitirá calcular la masa (y la energía, que cumple lo mismo)
en paralelo en el punto 3: cada mitad calcula su parte y se suman.

### Terminación y ausencia de efectos

Igual que en la sección 5: `map` y `sum` recorren $n$ elementos una vez cada
uno, y $s$ no se modifica.

### Comprobación con las pruebas

Pruebas dadas:

| Prueba | Entrada | Esperado |
|---|---|---|
| suma las masas | `s`: masas 2 y 2 | $4$ |
| sistema vacío | `vacio` | $0$ |

Pruebas propias:

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| suma de `tres` | masas 1, 2, 3 | $6$ | la suma de la especificación |
| masas decimales | $0{,}5$, $0{,}25$, $0{,}25$ | $1$ | funciona con decimales |
| no depende del movimiento | `pasoSecuencial(tres, 5.0)` | $6$ | solo cuentan las masas |
| partes y todo | `tres` partido en $[0,1)$ y $[1,3)$ | $1 + 5 = 6$ | $M(x \mathbin{+\!\!+} y) = M(x) + M(y)$ |
| rango en `grande` | 1000 masas entre $0{,}1$ y $5{,}1$ | entre $100$ y $5100$ | cota de la suma |

---

## 7. `energiaParalela` (punto 3)

### Especificación y programa

Se conserva la especificación $E(s)$ de la sección 5:

```math
E(s) = \sum_{i=0}^{n-1} \tfrac12 m_i |\vec v_i|^2
```

El programa completo está en la sección 7 de `proceso.md`. Su auxiliar local
`resolver(d, k)` usa `energiaSecuencial(s.slice(d, k))` si el tramo mide a lo
sumo `umb = math.max(umbral, 1)`; de lo contrario usa `parallel` y suma
`izq + der`. Hay que demostrar que devuelve $E(s)$ para cualquier umbral.

### Demostración por inducción fuerte sobre $L = k - d$

**Caso base: $L \leq \text{umb}$.** La función secuencial ya demostrada
calcula exactamente la suma del tramo:

```math
\text{resolver}(d,k) \rightarrow E(s[d:k])
```

Para $L = 0$, la suma vacía vale cero.

**Caso de inducción: $L > \text{umb}$.** Como $\text{umb} \geq 1$, ambos
subtramos son menores que $L$. Sea $m = d + \lfloor L/2 \rfloor$. Por la HI
en las dos ramas de `parallel`:

```math
\text{resolver}(d,k)
\rightarrow \text{resolver}(d,m) + \text{resolver}(m,k)
= \sum_{i=d}^{m-1} e(a_i) + \sum_{i=m}^{k-1} e(a_i)
= \sum_{i=d}^{k-1} e(a_i)
```

Los intervalos son disjuntos y cubren el tramo, incluso si $L$ es impar.
La llamada inicial `resolver(0, s.length)` calcula entonces $E(s)$.

### Combinación, terminación y ausencia de efectos

Sobre los reales, la suma es asociativa y tiene neutro cero:

```math
(a+b)+c = a+(b+c), \qquad a+0 = a
```

Con `Double`, reagrupar puede cambiar el redondeo; por eso las pruebas usan
`cerca` y la igualdad matemática no exige igualdad bit a bit.

Cada división reduce estrictamente $L$ hasta una hoja, incluidas las entradas
con umbral cero o negativo gracias a `umb`. Las hojas son cálculos finitos.
Solo se lee `s`; `slice` crea recortes propios y no hay acumuladores
compartidos entre tareas.

---

## 8. `centroDeMasaParalelo` (punto 3)

### Especificación y programa

Sean $\vec r_i$ la posición y $m_i$ la masa de la partícula $i$:

```math
Q(s) = \sum_{i=0}^{n-1} m_i \vec r_i, \qquad
M(s) = \sum_{i=0}^{n-1} m_i, \qquad
C(s) = \begin{cases}
\vec 0 & M(s)=0,\\
Q(s)/M(s) & M(s)\ne 0.
\end{cases}
```

El programa de la sección 8 de `proceso.md` calcula el par $(Q,M)$ con un
`foldLeft` en las hojas y lo combina con
`(izq._1 + der._1, izq._2 + der._2)`. Después comprueba `masa == 0.0` o
calcula `suma * (1.0 / masa)`. Hay que demostrar que devuelve $C(s)$.

### Lema: el auxiliar devuelve la suma ponderada y la masa

**Dentro de una hoja.** Tras procesar $j$ partículas del tramo, el par del
`foldLeft` es:

```math
\left(\sum_{i=d}^{d+j-1} m_i\vec r_i,\ \sum_{i=d}^{d+j-1}m_i\right)
```

Para $j=0$ coincide con `(Vector2(0, 0), 0.0)`. Al incorporar la siguiente
partícula, el cuerpo suma $m_{d+j}\vec r_{d+j}$ y $m_{d+j}$ a cada componente,
respectivamente. Por inducción sobre $j$, el par final es $(Q,M)$ del tramo.

**Entre ramas.** Por inducción fuerte sobre el tamaño del tramo, suponemos
que ambos hijos devuelven sus pares correctos. Combinarlos suma todos los
términos exactamente una vez:

```math
(Q(s[d:m])+Q(s[m:k]),\ M(s[d:m])+M(s[m:k]))
= (Q(s[d:k]),\ M(s[d:k]))
```

El caso base es la hoja demostrada arriba. Por tanto, `resolver(0, s.length)`
devuelve $(Q(s),M(s))$. Si $M(s)=0$ se devuelve el origen; en otro caso,
`suma * (1.0 / masa)` es $Q(s)/M(s)$, como exige la especificación.

### Asociatividad de la combinación

Definimos $(Q_1,M_1) \oplus (Q_2,M_2) = (Q_1+Q_2,M_1+M_2)$. Entonces:

```math
((Q_1,M_1)\oplus(Q_2,M_2))\oplus(Q_3,M_3)
= ((Q_1+Q_2)+Q_3,(M_1+M_2)+M_3)
= (Q_1+(Q_2+Q_3),M_1+(M_2+M_3))
= (Q_1,M_1)\oplus((Q_2,M_2)\oplus(Q_3,M_3))
```

El neutro es $(\vec 0,0)$. La demostración es sobre los reales; las
coordenadas y masas `Double` tienen las mismas limitaciones de redondeo que
la energía. La división no forma parte de $\oplus$: hacerla antes perdería
la información necesaria para combinar mitades con masas diferentes.

### Terminación y ausencia de efectos

Las divisiones reducen el tamaño hasta `umb`, como en la sección 7. Cada
`foldLeft` recorre un tramo finito; incluso la hoja vacía devuelve su neutro.
Los pares y los vectores son valores nuevos. No se escribe en `s` y una rama
sin masa no provoca una división entre cero.

### Comprobación con las cinco pruebas propias de P3

Están en `SimulacionParticulasPuntos3y4Test`, dentro del archivo de pruebas
existente. No usan `centroDeMasaSecuencial` como resultado esperado.

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| energía de cinco partículas | masas $0{,}5,2,4,0,3$, velocidades $(-2,0),(0,3),(1,2),(7,0),(0,0)$ | $1+9+10=20$ | tamaño impar, masa cero y umbrales $-4,0,1,2,5,9$ |
| cambio de posiciones | energías $6$ y $2$, traslación $(10,-7)$ | $8$ en ambos sistemas | independencia de la posición y conservación del original |
| centro en dos coordenadas | posiciones $(-4,2),(2,-2),(6,4)$, masas $2,1,3$ | $(2,7/3)$ | ponderación y umbrales $-3,0,1,2,3,8$ |
| mitad sin masa | dos masas cero, luego $0{,}5$ en $(-3,5)$ y $1{,}5$ en $(1,1)$ | $(0,2)$ | combinación con el neutro y conservación del original |
| sistema no vacío sin masa | tres posiciones distintas, masas cero | origen | protección de la división final |

Las pruebas dadas también cubren el vacío y comparan energías de un sistema
grande. La prueba dada que obtiene el centro esperado llamando al centro
secuencial depende de que se complete P2.

---

## 9. `trabajo` (punto 4)

### Especificación, programa y demostración

Para $n \geq 0$, el modelo cuenta una operación por partícula: $W(n)=n$.

```scala
def trabajo(n: Int): Int = {
  n
}
```

Por sustitución, `trabajo(n)` evalúa directamente a $n=W(n)$. No suma ni
multiplica enteros, así que tampoco desborda al recibir `Int.MaxValue`.
Termina en un número fijo de pasos y no tiene efectos.

---

## 10. `profundidad` (punto 4)

### Especificación y programa

Para $n \geq 0$ y $u=\max(\text{umbral},1)$, el camino crítico incluye el
trabajo secuencial de la hoja y una unidad por partición:

```math
D(n,u)=\begin{cases}
n & n\leq u,\\
1+\max(D(\lfloor n/2\rfloor,u),D(n-\lfloor n/2\rfloor,u)) & n>u.
\end{cases}
```

El programa completo está en la sección 10 de `proceso.md`: `resolver`
devuelve `tamano` en una hoja; en un nodo interno suma uno al máximo de los
dos llamados. No descarta el sobrante de un tamaño impar.

### Demostración por inducción fuerte sobre $n$

**Caso base: $n\leq u$.** `resolver(n)` devuelve $n=D(n,u)$, incluido $n=0$.

**Caso de inducción: $n>u$.** Sea $a=\lfloor n/2\rfloor$ y $b=n-a$.
Ambos son positivos y menores que $n$. Por la HI:

```math
\text{resolver}(n) \rightarrow 1+\max(\text{resolver}(a),\text{resolver}(b))
=1+\max(D(a,u),D(b,u))=D(n,u)
```

La llamada inicial `resolver(n)` da entonces la profundidad especificada.
Como el tamaño baja estrictamente hasta una hoja, el cálculo termina para
cualquier umbral corregido. No modifica datos ni ejecuta la simulación.

### Ejemplo con ramas de diferente costo

```math
D(6,5)=1+\max(3,3)=4, \qquad
D(11,5)=1+\max(5,4)=6
```

```math
D(12,5)=1+\max(4,4)=5, \qquad
D(23,5)=1+\max(6,5)=7
```

La rama menor puede dejar una hoja secuencial más costosa. Evaluar solamente
la mitad mayor no implementa esta recurrencia.

---

## 11. `tiempoEstimado` (punto 4)

### Especificación, programa y demostración

Para $n\geq 0$ y $p>0$ procesadores:

```math
T(n,p,u)=D(n,u)+\frac{W(n)}{p}
```

```scala
def tiempoEstimado(n: Int, p: Int, umbral: Int): Double = {
  profundidad(n, umbral) + trabajo(n).toDouble / p
}
```

Por las secciones 9 y 10, los llamados calculan $W$ y $D$. Convertir el
trabajo a `Double` antes de dividir conserva la parte fraccionaria del
cociente; sumarla a $D$ da la fórmula. La precisión es la de `Double`.
Por ejemplo, $D(13,3)=1+\max(4,4)=5$, así que
$T(13,8,3)=5+13/8=6{,}625$. Las funciones llamadas terminan y no tienen
efectos; esta tampoco los tiene.

El dominio del modelo es tamaño no negativo y procesadores positivos. No se
añaden reglas de rechazo o normalización para tamaños negativos ni para
procesadores no positivos, que no están especificadas por las pruebas dadas.

### Comprobación con las cinco pruebas propias de P4

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| trabajo vacío y grande | $0$, `Int.MaxValue` | los mismos valores | $W(n)=n$, sin cálculo que desborde |
| rama menor más costosa | $(11,5),(12,5),(23,5)$ | $6,5,7$ | máximo de ambas ramas |
| división impar | $(9,2)$ | $5$ | conserva el elemento sobrante |
| vacío y umbral no positivo | $n=0$ con $u=-2,0,1,4$; $n=5$ con $u=-2,0,1$ | $0$ y $4$, respectivamente | caso base y terminación |
| cociente fraccionario | $(13,8,3)$ y $(0,3,2)$ | $6{,}625$ y $0$ | división flotante y composición |

Las pruebas dadas comprueban además $D(1024,1024)=1024$,
$D(1024,512)=513$, $D(1024,1)=11$ y la fórmula con uno y cuatro procesadores.

---

## 12. `aceleracion` (punto 5)

### Especificación, programa y demostración

Para tiempos positivos, finitos y expresados en la misma unidad:

```math
S(t_s,t_p)=t_s/t_p.
```

```scala
def aceleracion(tSecuencial: Double, tParalelo: Double): Double = {
  tSecuencial / tParalelo
}
```

La única expresión implementa la definición con la precisión de `Double`.
Para un cambio de unidad por $c>0$, $(ct_s)/(ct_p)=t_s/t_p$ sobre los reales;
con coma flotante se compara con tolerancia. Por ejemplo, `0.018 / 0.006`
puede dar `2.9999999999999996` en lugar de `3.0`. No se añaden excepciones
para tiempos cero: fuera del dominio experimental positivo rigen las reglas
de `Double`, incluidos infinito y NaN.

---

## 13. `umbralConveniente` (punto 5)

### Especificación y programa

Con candidatos vacíos devuelve el tamaño del sistema. Con candidatos no
vacíos devuelve un candidato cuyo tiempo mediano observado sea mínimo:

```scala
def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int = {
  if (umbrales.isEmpty) s.length
  else umbrales.map(u => (u, medir(3) { pasoParalelo(s, dt, u) })).minBy(_._2)._1
}
```

### Demostración

En el caso vacío, la primera rama devuelve exactamente `s.length`, incluido
cero para un sistema vacío, y la rama que mide no se evalúa.

En el caso no vacío, `map` conserva un par por candidato y su orden. Cada
segunda componente se obtiene evaluando `pasoParalelo(s, dt, u)` con los
mismos datos y `dt` recibidos. La lista de pares es no vacía, así que `minBy`
está definido para tiempos finitos y el par elegido tiene una segunda
componente no mayor que la de los demás. Su primera componente pertenece a
la lista original. En empate se conserva el primero. La terminación depende
de que cada medición termine; cada paso paralelo termina por el argumento de
P1. No se modifican las partículas de entrada.

Esto prueba la selección del mínimo medido, no que ese umbral gane siempre.
Las pruebas no fijan tiempos ni ganador para candidatos distintos.

### Comprobación con las cinco pruebas propias de P5

| Prueba | Entrada | Esperado | Qué comprueba |
|---|---|---|---|
| mejora decimal | tiempos 7,5 y 3 | 2,5 con tolerancia | cociente fraccionario |
| paralelo más lento | tiempos 12 y 48 | 0,25 | conserva ralentizaciones |
| cambio de unidad | 18/6 y 0,018/0,006 | diferencia menor que $10^{-9}$ | redondeo flotante |
| candidatos vacíos | sistemas de 0 y 7 partículas | 0 y 7 sin medir | ambos casos de respaldo |
| candidato único | dos partículas, `dt=2`, `List(1)` | umbral 1, posiciones (-3,8) y (-2,-2) | paso completo, tres repeticiones e inmutabilidad |

La quinta prueba sustituye solamente `medir` para inspeccionar el bloque y
su argumento de repeticiones; no sustituye `pasoParalelo` ni inventa un ganador
entre umbrales distintos. La prueba original de pertenencia ejecuta además
la búsqueda real con `List(16,128,1024)`.

### Verificación ejecutada

```powershell
.\gradlew.bat :app:test --tests taller.SimulacionParticulasPuntos3y4Test --tests taller.SimulacionParticulasPunto5Test --offline --console=plain
.\gradlew.bat :app:test :app:scalastyleMainCheck :app:scalastyleTestCheck --offline --console=plain --rerun-tasks --continue
```

La selección P3/P4/P5 pasó: 15 pruebas propias (5 por punto). La suite completa
ejecutó 81 pruebas: 77 pasaron y 4 fallaron con `scala.NotImplementedError`.
Tres fallos corresponden a `centroDeMasaSecuencial` de P2 y uno a la prueba
original de P3 que llama esa referencia secuencial pendiente. No son fallos
de las pruebas propias ni de P5. Scalastyle procesó cuatro archivos principales
y un archivo de pruebas, con cero errores y cero advertencias en ambos grupos.
P2 se conserva pendiente.

La recuperación directa del 25 de noviembre de 2026 repitió la suite completa
después del benchmark: 81 pruebas, 77 correctas y los mismos cuatro
`NotImplementedError`; ambos controles Scalastyle volvieron a dar cero errores
y advertencias. El benchmark terminó correctamente en 53 segundos. La tabla
actualizada y la dispersión de sus controles están en `paralelizacion.md`:
la corrección funcional de P5 no implica estabilidad experimental validada.
