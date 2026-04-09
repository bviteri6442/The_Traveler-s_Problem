# The Traveler's Problem — TSP con Algoritmo A*

> Aplicación Android desarrollada en **Kotlin** que resuelve el **Problema del Viajero (Traveling Salesman Problem)** mediante el algoritmo de búsqueda informada **A\***. Desarrollada para la asignatura de **Inteligencia Artificial / Algoritmos de Búsqueda**.

---

## Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Estructura del Proyecto](#estructura-del-proyecto)
3. [Modelado del Estado](#modelado-del-estado)
4. [Representación del Nodo](#representación-del-nodo)
5. [Clases y Métodos](#clases-y-métodos)
6. [Algoritmo A*](#algoritmo-a)
7. [Función Heurística](#función-heurística)
8. [Interfaz de Usuario](#interfaz-de-usuario)
9. [Estadísticas de Búsqueda](#estadísticas-de-búsqueda)
10. [Visualización del Grafo](#visualización-del-grafo)
11. [Análisis de Complejidad](#análisis-de-complejidad)
12. [Configuración del Proyecto](#configuración-del-proyecto)

---

## Descripción General

El **Problema del Viajero (TSP)** consiste en encontrar el circuito de mínimo costo que, partiendo de una ciudad de origen, visite exactamente una vez cada ciudad del conjunto y regrese al punto de partida.

Esta aplicación resuelve el problema para **entre 2 y 8 ciudades**, con una interfaz de 3 pasos que guía al usuario desde la configuración de ciudades hasta la visualización del resultado óptimo.

### Características principales

| Característica | Detalle |
|---|---|
| Algoritmo | A* (búsqueda informada con heurística admisible) |
| Lenguaje | Kotlin |
| Plataforma | Android (minSdk 24, targetSdk 36) |
| Ciudades soportadas | 2 – 8 |
| Estructura de datos central | Cola de prioridad (min-heap) + máscara de bits |
| Garantía de optimalidad | Sí — la heurística es admisible |

---

## Estructura del Proyecto

```
The_Traveler-s_Problem/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/the_travelers_problem4/
│   │   │   │   └── MainActivity.kt          # Toda la lógica del algoritmo y la UI
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml    # Layout de 3 pasos (CardViews)
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   │   └── ExampleUnitTest.kt
│   │   └── androidTest/
│   │       └── ExampleInstrumentedTest.kt
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

> Todo el núcleo del sistema reside en **`MainActivity.kt`** (327 líneas). Es una arquitectura de actividad única que encapsula los modelos de datos, el algoritmo A\*, la lógica de UI y la visualización gráfica.

---

## Modelado del Estado

### ¿Qué es un estado?

Un **estado** describe la situación del viajero en un instante determinado de la búsqueda. En el TSP, un estado queda definido completamente por dos datos:

1. **Ciudad actual** — dónde se encuentra el viajero ahora mismo.
2. **Conjunto de ciudades visitadas** — cuáles ciudades ya fueron recorridas.

### Representación en código

El estado **no es una clase separada**; está distribuido en los campos del `Node` y en las estructuras globales de `MainActivity`:

```kotlin
// Ciudad actual (índice 0..N-1)
val currentCity: Int

// Máscara de bits: el bit i vale 1 si la ciudad i fue visitada
val visitedMask: Int
```

#### Ejemplo con 4 ciudades (A=0, B=1, C=2, D=3)

| Situación | currentCity | visitedMask (binario) | visitedMask (decimal) |
|---|---|---|---|
| Inicio en A, solo A visitada | 0 | `0001` | 1 |
| En B, visitadas A y B | 1 | `0011` | 3 |
| En C, visitadas A, B y C | 2 | `0111` | 7 |
| En D, todas visitadas | 3 | `1111` | 15 |

#### Estado objetivo

```kotlin
val allVisited = (1 shl n) - 1  // todos los bits a 1
// Para n=4: allVisited = 15 = 0b1111
```

La búsqueda concluye cuando `visitedMask == allVisited`.

### Estructuras de datos globales del estado

```kotlin
// Lista de ciudades ingresadas por el usuario
private val citiesList = mutableListOf<City>()

// Matriz simétrica N×N de distancias entre ciudades (en km)
private var distanceMatrix = Array(0) { DoubleArray(0) }

// Mapa de memoización: (ciudadActual, máscara) → menor costo g conocido
val minCostToState = mutableMapOf<Pair<Int, Int>, Double>()
```

#### Matriz de distancias

- **Tipo**: `Array<DoubleArray>` — arreglo bidimensional de dobles.
- **Tamaño**: N × N donde N es el número de ciudades.
- **Simetría**: `distanceMatrix[i][j] == distanceMatrix[j][i]`.
- **Llenado**: El usuario ingresa solo el triángulo superior (pares `i < j`); el código espeja los valores.

```kotlin
distanceMatrix[i][j] = value
distanceMatrix[j][i] = value   // simetría garantizada
```

---

## Representación del Nodo

### Clase `Node`

Cada nodo del árbol de búsqueda es una instancia de la clase `Node`, definida como `data class` dentro de `MainActivity`:

```kotlin
data class Node(
    val currentCity: Int,    // Ciudad donde está el viajero
    val visitedMask: Int,    // Bitmask de ciudades visitadas
    val g: Double,           // Costo real acumulado desde el origen
    val h: Double,           // Estimación heurística del costo restante
    val path: List<Int>      // Secuencia de índices de ciudades recorridas
) : Comparable<Node> {
    val f: Double get() = g + h          // Costo total estimado
    override fun compareTo(other: Node): Int = f.compareTo(other.f)
}
```

### Significado de cada campo

| Campo | Tipo | Descripción |
|---|---|---|
| `currentCity` | `Int` | Índice (0..N-1) de la ciudad actual |
| `visitedMask` | `Int` | Bitmask de ciudades visitadas |
| `g` | `Double` | Distancia real recorrida desde la ciudad 0 |
| `h` | `Double` | Estimación del costo mínimo restante (heurística) |
| `f` | `Double` | `g + h` — criterio de prioridad en la cola |
| `path` | `List<Int>` | Lista ordenada de ciudades ya visitadas |

### Relación entre nodos (árbol de búsqueda)

Los nodos **no guardan una referencia explícita al padre**. En su lugar, cada nodo carga una copia inmutable de `path` que acumula el historial completo:

```
Nodo raíz:   path = [0]
  └─ Nodo hijo (ciudad 1): path = [0, 1]
       └─ Nodo nieto (ciudad 2): path = [0, 1, 2]
            └─ Nodo hoja (ciudad 3 + retorno): path = [0, 1, 2, 3, 0]
```

### `Comparable<Node>` — orden en la cola de prioridad

La implementación de `compareTo` ordena los nodos de **menor a mayor `f`**, haciendo que la `PriorityQueue` de Java se comporte como una **cola de mínimos (min-heap)**:

```kotlin
override fun compareTo(other: Node): Int = f.compareTo(other.f)
```

---

## Clases y Métodos

### Clase `City`

```kotlin
data class City(val name: String, val id: Int)
```

Modelo simple para representar una ciudad. `id` corresponde al índice en `citiesList` y en la `distanceMatrix`.

---

### Clase `MainActivity`

Clase principal que extiende `AppCompatActivity`. Concentra todo: modelos, algoritmo, UI y visualización.

#### Atributos de instancia

```kotlin
private val citiesList        = mutableListOf<City>()
private var distanceMatrix    = Array(0) { DoubleArray(0) }
private var expandedNodes     = 0
private var generatedNodes    = 0
private var prunedNodes       = 0
```

#### Métodos del ciclo de vida Android

| Método | Línea | Descripción |
|---|---|---|
| `onCreate()` | 53 | Punto de entrada de la actividad; llama a `setupUI()` |

#### Métodos de UI

| Método | Línea | Descripción |
|---|---|---|
| `setupUI()` | 61 | Enlaza vistas y registra listeners de botones |
| `addCity(name)` | 101 | Crea un objeto `City`, lo agrega a `citiesList` y renderiza un `Chip` eliminable |
| `showStep2()` | 117 | Oculta el Paso 1, muestra el Paso 2; inicializa `distanceMatrix` y genera dinámicamente los campos de distancia |
| `captureDistances()` | 148 | Lee los `EditText` por su `tag` y rellena la `distanceMatrix` simétricamente |
| `solveAndShowResults()` | 165 | Resetea contadores, ejecuta `runAStar()` y muestra resultados en el Paso 3 |
| `resetAll()` | 317 | Limpia toda la sesión y regresa al Paso 1 |

#### Métodos del algoritmo

| Método | Línea | Descripción |
|---|---|---|
| `runAStar()` | 192 | Implementación completa del algoritmo A* con poda por memoización |
| `estimateH(current, mask)` | 243 | Heurística admisible: distancia mínima a la ciudad no visitada más cercana |

#### Métodos de visualización

| Método | Línea | Descripción |
|---|---|---|
| `drawGraph(path)` | 262 | Dibuja el grafo óptimo sobre un `Bitmap` de 1000×1000 px |

---

## Algoritmo A*

### Implementación — `runAStar()` (línea 192)

```kotlin
private fun runAStar(): Node? {
    val n = citiesList.size
    val allVisited = (1 shl n) - 1
    val pq = PriorityQueue<Node>()
    val minCostToState = mutableMapOf<Pair<Int, Int>, Double>()

    // 1. Nodo inicial: en ciudad 0, solo ciudad 0 visitada
    val startNode = Node(0, 1 shl 0, 0.0, estimateH(0, 1 shl 0), listOf(0))
    pq.add(startNode)
    generatedNodes++

    while (pq.isNotEmpty()) {
        val current = pq.poll()!!     // Extrae nodo con menor f
        expandedNodes++

        // 2. Poda por estado dominado
        val state = Pair(current.currentCity, current.visitedMask)
        if (minCostToState[state] != null && minCostToState[state]!! < current.g) {
            prunedNodes++
            continue
        }
        minCostToState[state] = current.g

        // 3. Prueba de meta
        if (current.visitedMask == allVisited) {
            val finalG = current.g + distanceMatrix[current.currentCity][0]
            return Node(0, allVisited, finalG, 0.0, current.path + 0)
        }

        // 4. Expansión: generar sucesores para ciudades no visitadas
        for (next in 0 until n) {
            if ((current.visitedMask and (1 shl next)) == 0) {
                val newG = current.g + distanceMatrix[current.currentCity][next]
                pq.add(Node(
                    next,
                    current.visitedMask or (1 shl next),
                    newG,
                    estimateH(next, current.visitedMask or (1 shl next)),
                    current.path + next
                ))
                generatedNodes++
            }
        }
    }
    return null
}
```

### Flujo paso a paso

```
1. INICIALIZACIÓN
   ├── n = número de ciudades
   ├── allVisited = (1 << n) - 1       ← bitmask objetivo
   ├── pq = PriorityQueue (min-heap)
   ├── minCostToState = {}              ← mapa de memoización
   └── Insertar nodo raíz (ciudad 0)

2. BUCLE PRINCIPAL
   └── Mientras pq no esté vacía:
       ├── current = pq.poll()          ← nodo con menor f = g + h
       ├── expandedNodes++
       │
       ├── PODA: ¿ya conocemos este (ciudad, máscara) con menor g?
       │         Sí → prunedNodes++, descartar
       │         No → registrar en minCostToState
       │
       ├── META: ¿visitedMask == allVisited?
       │         Sí → añadir retorno al origen, DEVOLVER solución
       │
       └── EXPANSIÓN: para cada ciudad next no visitada
           ├── newG = current.g + distancia[current][next]
           ├── crear Node(next, máscara|next, newG, h, path+next)
           └── pq.add(nodo)   generatedNodes++

3. RETORNAR null si no hay solución
```

### Poda por memoización

El mapa `minCostToState` guarda el **menor costo `g` conocido** para cada par `(ciudadActual, visitedMask)`. Si al sacar un nodo de la cola su costo `g` es mayor que el ya registrado, el nodo se descarta sin expandirlo:

```kotlin
if (minCostToState[state] != null && minCostToState[state]!! < current.g) {
    prunedNodes++
    continue
}
```

Esto evita explorar ramas que ya sabemos que son subóptimas para llegar al mismo estado.

---

## Función Heurística

### Implementación — `estimateH()` (línea 243)

```kotlin
private fun estimateH(current: Int, mask: Int): Double {
    var h = 0.0
    val n = citiesList.size
    var minToUnvisited = Double.MAX_VALUE
    var anyUnvisited = false

    for (i in 0 until n) {
        if ((mask and (1 shl i)) == 0) {       // ¿ciudad i no visitada?
            anyUnvisited = true
            if (distanceMatrix[current][i] < minToUnvisited)
                minToUnvisited = distanceMatrix[current][i]
        }
    }
    if (anyUnvisited) h += minToUnvisited
    return h
}
```

### Propiedades de la heurística

| Propiedad | ¿Cumple? | Justificación |
|---|---|---|
| **Admisible** | Sí | El mínimo a una ciudad no visitada nunca supera el costo real restante |
| **Consistente** | Sí | `h(n) ≤ c(n, n') + h(n')` se mantiene al avanzar ciudad a ciudad |
| **Garantía de optimalidad** | Sí | A* con heurística admisible siempre encuentra la solución óptima |

**Intuición**: en el mejor caso posible, el viajero solo necesita ir a la ciudad no visitada más cercana. Cualquier ruta real costará al menos esa distancia, por lo que la heurística nunca sobreestima.

---

## Interfaz de Usuario

La UI sigue un flujo lineal de **3 pasos** implementados con `CardView`:

### Paso 1 — Configuración de ciudades

- `EditText` para ingresar el nombre de cada ciudad.
- Botón **AGREGAR CIUDAD** → llama a `addCity()`.
- `ChipGroup` muestra las ciudades agregadas; cada `Chip` tiene ícono de borrado.
- Contador `"X / 8 ciudades"`.
- Botón **SIGUIENTE** (habilitado cuando hay ≥ 2 ciudades).

### Paso 2 — Ingreso de distancias

- `showStep2()` genera dinámicamente un `EditText` etiquetado por `tag = "dist_i_j"` para cada par `(i, j)` con `i < j`.
- `captureDistances()` lee esos valores y llena la `distanceMatrix`.
- Botón **RESOLVER** → ejecuta el algoritmo.

### Paso 3 — Resultados

| Componente | Descripción |
|---|---|
| `ImageView` (ivGraph) | Grafo visual del circuito óptimo |
| `tvBestPath` | Ruta en formato "Ciudad A → Ciudad B → ... → Ciudad A" |
| `tvTotalDistance` | Distancia óptima en km |
| `tvStats` | Nodos expandidos, generados y podados |

---

## Estadísticas de Búsqueda

| Contador | Cuándo se incrementa | Significado |
|---|---|---|
| `generatedNodes` | Al agregar un nodo a la `PriorityQueue` | Total de nodos creados durante la búsqueda |
| `expandedNodes` | Al extraer un nodo de la `PriorityQueue` | Nodos efectivamente explorados |
| `prunedNodes` | Al descartar un nodo por estado dominado | Ramas eliminadas por la poda |

**Relación esperada**: `generatedNodes ≥ expandedNodes + prunedNodes`

---

## Visualización del Grafo

### Implementación — `drawGraph()` (línea 262)

El grafo se dibuja sobre un `Bitmap` de **1000 × 1000 px** con la API `Canvas` de Android.

#### Distribución de ciudades

Las ciudades se colocan en posiciones equidistantes sobre una circunferencia de radio 350 px centrada en (500, 500):

```kotlin
val angle = Math.toRadians((i * 360.0 / n) - 90)  // -90° para empezar arriba
val x = centerX + radius * cos(angle)
val y = centerY + radius * sin(angle)
```

#### Orden de dibujo

1. **Aristas** (líneas azules `#1976D2`, grosor 5 px) entre ciudades consecutivas del `path`.
2. **Etiquetas de distancia** (texto gris oscuro) centradas sobre cada arista.
3. **Nodos** (círculos de 40 px de radio):
   - Ciudad origen (índice 0): rojo `#D32F2F`
   - Resto de ciudades: verde azulado `#00796B`
4. **Nombres de ciudad** — primeras 2 letras en blanco dentro del círculo; nombre completo en negro debajo.

---

## Análisis de Complejidad

| Aspecto | Valor |
|---|---|
| **Estados posibles** | N × 2^N |
| **Complejidad temporal (peor caso)** | O(N! · log(2^N)) |
| **Complejidad espacial** | O(2^N) — nodos en cola y mapa de memoización |
| **Para 8 ciudades** | ~2 048 estados máximos |

La poda por memoización reduce significativamente el número de nodos expandidos en la práctica.

---

## Configuración del Proyecto

### Dependencias (`app/build.gradle.kts`)

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

### Versiones SDK

| Parámetro | Valor |
|---|---|
| `compileSdk` | 36 |
| `minSdk` | 24 |
| `targetSdk` | 36 |
| `versionName` | 1.0 |

### Requisitos para compilar

- Android Studio Hedgehog o superior
- JDK 17+
- SDK Platform 36

---

*Desarrollado para la asignatura de Inteligencia Artificial — Algoritmos de Búsqueda.*
