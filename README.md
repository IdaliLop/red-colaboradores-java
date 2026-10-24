# Analizador de red de colaboradores

Proyecto de estructuras de datos y algoritmia para analizar una red de relaciones en Java 17+. El programa no usa librerias externas de grafos: la implementacion usa solo `java.util` y el lector CSV usa la API estandar de Java. JUnit 5 se usa exclusivamente para pruebas.

## Funcionalidad

- Lee una red en CSV UTF-8, con filas para registrar nodos y aristas.
- Guarda, en paralelo, una lista de adyacencia dirigida y la proyeccion no dirigida de la red.
- Encuentra componentes conexas debiles con DFS y BFS.
- Detecta ciclos dirigidos con DFS de tres colores y ciclos no dirigidos con BFS y seguimiento del padre.
- Calcula grados de entrada y salida.
- Busca el camino simple dirigido mas largo entre dos colaboradores indicados, con backtracking y poda por cota superior.
- Encuentra puntos de articulacion con el algoritmo de Tarjan sobre la proyeccion no dirigida.
- Imprime tablas en consola y escribe el mismo analisis en `resultados.txt`.

## Formato de entrada

Se eligio CSV porque es facil de inspeccionar, editar y producir en una hoja de calculo. El parser esta implementado con la biblioteca estandar y entiende campos entre comillas, comas dentro de campos y comillas escapadas. El formato permite registrar colaboradores aislados sin inventar una arista:

```csv
tipo,origen,destino
nodo,Ada
nodo,"Lia, del equipo B"
arista,Ada,Beto
arista,Beto,Cora
```

Las filas `nodo` tienen dos campos; las filas `arista` tienen tres. Una arista `A,B` representa `A -> B`. Al cargarla, tambien se agrega la conexion no dirigida `{A,B}`. Las aristas duplicadas se colapsan; los auto-lazos se conservan y se cuentan como ciclos.

## Estructuras y decisiones

Cada vertice es una clave de `HashMap<String, HashSet<String>>`; los conjuntos permiten evitar aristas repetidas y hacer inserciones y consultas promedio en O(1). El grafo dirigido mantiene vecinos de salida. El grafo no dirigido agrega ambos sentidos y se usa para componentes debiles, ciclos no dirigidos y articulaciones. Todos los pesos son unitarios.

Las colas y pilas de recorrido usan `ArrayDeque`: ofrece operaciones eficientes en ambos extremos y evita la sincronizacion innecesaria de `Stack`. Se evita `LinkedList`, que crea un objeto por nodo y tiene peor localidad de memoria para estos recorridos. `HashSet` se usa para marcar visitados y garantizar unicidad.

Las componentes se interpretan como componentes conexas debiles: se conectan vertices si existe una arista en cualquier sentido entre ellos. Esta interpretacion es coherente con la red de relaciones y con el grafo no dirigido derivado.

## Complejidad

Con `V` vertices y `E` aristas dirigidas (y `E'` aristas en la proyeccion no dirigida), las operaciones base de componentes, deteccion de ciclos, grados y articulaciones cuestan O(V + E) u O(V + E') segun el grafo recorrido, con O(V + E) de almacenamiento. Para reproducir el orden de las rutas y tablas, la implementacion ordena vertices y vecinos; ese costo eleva las rutinas de recorrido hasta O(V log V + E log V) y el calculo de grados hasta O(V log V + E).

La busqueda del camino simple mas largo es NP-dificil en general. El backtracking puede tomar tiempo factorial en el peor caso; la enumeracion base de rutas es O(V!), y el ordenamiento repetido de vecinos agrega un factor logaritmico, dando O(V! log V) como cota de esta implementacion. Usa O(V) espacio de recorrido, sin contar el grafo. La poda descarta una rama cuando `aristas actuales + vertices aun no visitados` no puede superar la mejor ruta ya encontrada. Esa es una cota superior valida porque cada vertice no visitado puede aportar como maximo una arista adicional a un camino simple.

## Requisitos

- JDK 17 o superior.
- Apache Maven 3.8 o superior para ejecutar JUnit 5.

## Compilar y probar

Desde el directorio del proyecto:

```bash
mvn test
mvn package
```

## Ejecutar

Los argumentos opcionales son: archivo CSV, archivo de salida, vertice inicial y vertice final. Si no se especifican, se usan `red.csv`, `resultados.txt` y el primer/ultimo identificador en orden alfabetico.

```bash
java -cp target/classes mx.edu.estructuras.red.Main red.csv resultados.txt Ada Kira
```

En Windows, puede escribirse la ruta de clases como `target\classes`.

El CSV de ejemplo tiene 14 nodos en dos componentes. Para generar el resultado de ejemplo se usa:

```bash
java -cp target/classes mx.edu.estructuras.red.Main red.csv resultados.txt Ada Kira
```

## Casos JUnit 5

La suite cubre grafo con ciclo, arbol, dos componentes, red de un nodo, red vacia, auto-lazo, grados, camino mas largo, nodo aislado y parseo de CSV con campos entrecomillados.
