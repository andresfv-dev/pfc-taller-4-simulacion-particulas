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
