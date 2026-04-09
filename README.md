# Problema del Viajero (TSP) - Solución con Algoritmo A*

Este proyecto es una aplicación de Android desarrollada en **Kotlin** que resuelve una versión simplificada del **Problema del Viajero (Traveling Salesman Problem - TSP)**. El objetivo es encontrar la ruta más corta que visite un conjunto de ciudades (entre 5 y 8) exactamente una vez y regrese al punto de partida.

---

## 1. Modelado del Estado

El **Estado** representa la situación específica del problema en un momento determinado de la búsqueda.

*   **Representación del Problema**: El problema se modela como un grafo completo ponderado donde cada nodo es una ciudad y cada arista tiene un peso que representa la distancia entre ellas.
*   **Estructuras de Datos**:
    *   **Matriz de Distancias (`distanceMatrix`)**: Una matriz bidimensional de tipo `DoubleArray` (tamaño $N \times N$) que almacena las distancias entre cada par de ciudades $i$ y $j$. Es una matriz simétrica.
    *   **Lista de Ciudades (`citiesList`)**: Una lista mutable de objetos `City` que almacena el nombre y el ID único de cada ciudad ingresada por el usuario.
    *   **Máscara de Bits (`visitedMask`)**: Un entero (Int) utilizado para representar el conjunto de ciudades visitadas de forma eficiente. Cada bit en la posición $i$ indica si la ciudad $i$ ha sido visitada (1) o no (0).

---

## 2. Representación del Nodo

Cada **Nodo** en el árbol de búsqueda representa un paso en la construcción de la ruta y contiene la información necesaria para que el algoritmo **A*** tome decisiones.

*   **Clase `Node`**:
    *   `currentCity`: El índice de la ciudad donde se encuentra el viajero actualmente.
    *   `visitedMask`: El estado de las ciudades visitadas hasta este punto.
    *   `g`: El **costo acumulado** (distancia real recorrida desde la ciudad de origen).
    *   `h`: El **costo estimado** o heurística (una estimación de la distancia mínima restante para completar el viaje).
    *   `f`: El costo total estimado ($f = g + h$). El algoritmo siempre expande el nodo con el valor $f$ más bajo.
    *   `path`: Una lista de enteros (`List<Int>`) que almacena la secuencia de ciudades visitadas para reconstruir la ruta final.

---

## 3. Lógica del Algoritmo A*

El algoritmo A* es una técnica de búsqueda informada que utiliza conocimiento específico del problema (heurística) para encontrar la solución óptima de manera eficiente.

### Funcionamiento:
1.  Se inicia en la primera ciudad agregada (Origen).
2.  Se utiliza una **Cola de Prioridad (`PriorityQueue`)** para gestionar los nodos, ordenándolos de menor a mayor valor de $f$.
3.  En cada iteración, se **expande** el mejor nodo: se generan nuevos nodos para cada ciudad no visitada.
4.  **Poda (Pruning)**: Si el algoritmo llega a un estado (misma ciudad actual y mismas ciudades visitadas) con un costo $g$ mayor a uno ya registrado, ese camino se descarta (se poda).
5.  **Heurística ($h$)**: Se calcula buscando la distancia a la ciudad no visitada más cercana. Es una heurística **admisible** porque nunca sobreestima el costo real, garantizando que el algoritmo encuentre la ruta óptima.

---

## 4. Estadísticas y Definiciones para la Defensa

Durante la ejecución, la app calcula estadísticas clave que suelen ser evaluadas:

*   **Nodos Generados**: Total de nodos creados y añadidos a la cola de prioridad. Representa la exploración potencial.
*   **Nodos Expandidos**: Nodos que fueron seleccionados de la cola para explorar sus vecinos. Representa el trabajo real realizado.
*   **Nodos Podados**: Caminos que fueron interrumpidos porque se demostró que no eran óptimos (existía una forma más barata de llegar al mismo estado).
*   **Implementación**: Toda la lógica reside en la clase `MainActivity.kt`, específicamente en el método `runAStar()`.

---

## 5. Interfaz de Usuario (UI)

La aplicación sigue un flujo de 3 pasos:
1.  **Configuración de Ciudades**: Interfaz dinámica para agregar entre 5 y 8 ciudades.
2.  **Ingreso de Distancias**: Campos de texto generados dinámicamente para definir los kilómetros entre cada par de ciudades.
3.  **Visualización de Resultados**:
    *   **Grafo Dinámico**: Dibujado en un `Bitmap` sobre un `ImageView`.
    *   **Ruta Óptima**: Texto con la secuencia de ciudades.
    *   **Distancia Total**: Suma final de los tramos.
    *   **Panel de Estadísticas**: Datos técnicos de la ejecución del algoritmo.

---
**Desarrollado para la asignatura de Inteligencia Artificial / Algoritmos de Búsqueda.**
