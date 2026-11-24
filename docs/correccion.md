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
