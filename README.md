# Análisis de la red de colaboradores

Proyecto del examen práctico de Estructuras de Datos y Algoritmia. Modela colaboradores como vértices y relaciones dirigidas como aristas; calcula comunidades, ciclos, grados, caminos simples máximos y colaboradores puente.

> **Versión documentada.** La entrega completa descrita aquí está en [red-colaboradores.zip](red-colaboradores.zip). Al extraerlo se crea la carpeta \`red-colaboradores/\`, con su propio README, POM, código fuente, pruebas, datos y reporte. La carpeta \`src/\` que ya está en la raíz del repositorio conserva una implementación anterior en Java; ambas versiones se describen por separado al final.

## Entrega y archivos

- [Paquete Java completo](red-colaboradores.zip): código, pruebas JUnit 5, datos de ejemplo, documentación y \`reporte.pdf\`.
- [CSV de la red de ejemplo](data/red.csv): versión tabular de los datos incluidos en el paquete.
- [Resultados de ejemplo](resultados.txt): análisis de la red de 16 colaboradores, con tres componentes débiles.
- El reporte profesional \`reporte.pdf\` está dentro del ZIP en \`red-colaboradores/reporte.pdf\`.

## Modelo y convenciones

Cada colaborador es un nodo. Una arista \`u → v\` significa que **u reporta a v**. El grafo es dirigido y no ponderado: cada arista tiene peso unitario y la longitud de un camino se mide por su número de aristas.

El programa conserva los sucesores y predecesores de cada nodo. Para conectividad, ciclos no dirigidos y puntos de articulación deriva una vista no dirigida: existe una conexión entre dos nodos si hay una arista en cualquiera de los sentidos. Esa vista colapsa las relaciones mutuas \`u → v\` y \`v → u\` en una sola conexión.

- **Grado de salida:** colaboradores a quienes el nodo reporta.
- **Grado de entrada:** colaboradores que reportan a ese nodo.
- **Componentes:** se reportan componentes débilmente conexas; se ignora el sentido de las aristas para decidir si dos colaboradores pertenecen a la misma comunidad.
- **Aristas repetidas:** se ignoran, para mantener un grafo simple.
- **Nodos aislados:** se pueden declarar explícitamente en JSON o con una fila de un solo campo en CSV.
- **Auto-lazos:** cuentan como ciclo dirigido y no dirigido y suman uno al grado de entrada y uno al de salida; no conectan nodos distintos ni alteran los puntos de articulación.

## Requisitos funcionales y algoritmos

| Requisito | Implementación | Clases principales | Complejidad |
|---|---|---|---|
| RF1. Cargar JSON o CSV | Valida el esquema, crea nodos declarados y nodos que aparezcan en aristas, e ignora aristas duplicadas. | \`CargadorRed\`, \`JsonMinimo\`, \`Grafo\` | O(n) para leer n caracteres; O(V + E) para construir el grafo, en promedio. |
| RF2. Comunidades aisladas | BFS sobre la vista no dirigida; devuelve las componentes de mayor a menor tamaño. | \`Componentes\` | O(V + Eᵤ) |
| RF2. Ciclos dirigidos | DFS iterativo con estados blanco, gris y negro. Una arista hacia un nodo gris identifica un ciclo y se reconstruye la ruta. | \`Ciclos\` | O(V + E) |
| RF2. Ciclos no dirigidos | DFS sobre la vista no dirigida; una arista a un nodo visitado que no sea el padre identifica un ciclo. | \`Ciclos\` | O(V + Eᵤ) |
| RF3. Grados de entrada y salida | Cuenta predecesores y sucesores en las listas de adyacencia y genera el ranking de influencia. | \`Grados\` | O(V + E) |
| RF4. Camino simple más largo | Backtracking entre cada par solicitado; calcula versiones dirigida y no dirigida. Usa cotas seguras para podar ramas. | \`CaminoMasLargo\` | O(V! · (V + E)) en el peor caso general. |
| RF5. Colaboradores puente | Tarjan/Hopcroft–Tarjan iterativo sobre la vista no dirigida; también calcula cuántos fragmentos quedan al eliminar un nodo. | \`PuntosArticulacion\` | O(V + Eᵤ) |
| RF6. Reporte | Imprime las mismas tablas en consola y en UTF-8 a \`resultados.txt\`. | \`Reporte\`, \`Main\` | O(t) para generar y escribir t caracteres, además del análisis. |

V es el número de nodos, E el número de aristas dirigidas, Eᵤ el número de conexiones únicas de la vista no dirigida y n la longitud del archivo de entrada.

### Justificación de estructuras de datos

El grafo usa \`LinkedHashMap\` y \`LinkedHashSet\`, variantes de \`HashMap\` y \`HashSet\`. Las tablas hash ofrecen inserción, consulta y pertenencia O(1) esperadas; las variantes enlazadas conservan el orden de inserción para que recorridos, ciclos y reportes sean reproducibles. Los conjuntos también impiden contar dos veces una arista idéntica.

Los sucesores permiten recorrer aristas salientes y calcular grados de salida; los predecesores permiten lo mismo para las entradas. Mantener ambos índices hace directas estas consultas. La vista no dirigida se construye bajo demanda, se almacena en caché y se invalida si cambia el grafo: así hay una única fuente de verdad para las aristas dirigidas y no se mantienen dos grafos mutables que puedan desincronizarse.

Los recorridos usan \`ArrayDeque\` como cola BFS y como pila explícita DFS. Evita la sincronización heredada por \`Stack\` de \`Vector\` y el objeto de enlace por elemento y acceso disperso de \`LinkedList\`. La pila explícita evita desbordar la pila de llamadas en DFS de grafos profundos. Los conjuntos de visitados proporcionan consultas esperadas O(1).

### Justificación del formato y del parser

JSON es el formato principal porque separa explícitamente las colecciones \`nodos\` y \`aristas\`. Esto representa sin ambigüedad los nodos aislados, distingue datos de estructura y permite extender registros. \`JsonMinimo\` implementa un parser de descenso recursivo para el esquema JSON requerido, en O(n) respecto al tamaño del documento, sin incorporar una biblioteca externa.

También se admite CSV para editar o producir datos desde una hoja de cálculo. El cargador acepta encabezado opcional, comentarios iniciados con \`#\`, campos entrecomillados y filas que declaran un nodo aislado. No se utilizan bibliotecas externas de grafos; el código de producción utiliza el JDK estándar. JUnit 5 solo es dependencia de pruebas.

### Justificación de la poda del camino más largo

El camino buscado es simple: no puede repetir nodos, y se mide en aristas. En grafos generales, el problema es NP-difícil; el número de caminos posibles puede crecer factorialmente, por lo que el backtracking no tiene garantía polinomial.

En cada estado, sea L la longitud actual y R el conjunto de nodos no visitados que todavía son alcanzables desde el nodo actual usando únicamente nodos no visitados. Una prolongación simple puede entrar a cada nodo de R como máximo una vez; por tanto, puede añadir a lo sumo |R| aristas. La poda es segura cuando:

1. El destino no pertenece a R: ninguna continuación válida puede alcanzarlo.
2. L + |R| es menor o igual que la mejor longitud ya encontrada: la rama no puede producir una solución estrictamente mejor.

Primero se evalúa la cota barata |R| ≤ V − número de nodos del camino; después se calcula la alcanzabilidad restante para una cota más precisa. Ambas podas descartan solo ramas incapaces de mejorar el resultado. La suite compara esta búsqueda con una búsqueda exhaustiva sin poda. En un DAG dirigido, la clase ofrece además una solución especializada por programación dinámica en O(V + E); esto no cambia el peor caso del problema general.

## Formatos de entrada

### JSON

El objeto admite las claves opcionales \`nodos\` y \`aristas\`. Los identificadores declarados permiten representar colaboradores aislados:

\`\`\`json
{
  "nodos": ["Ana", "Beto", "Paula"],
  "aristas": [
    {"origen": "Beto", "destino": "Ana"}
  ]
}
\`\`\`

### CSV

Cada fila representa una arista \`origen,destino\`. Una fila con un solo valor o destino vacío declara un nodo aislado:

\`\`\`csv
origen,destino
Beto,Ana
Paula,
\`\`\`

Si una arista menciona un nodo no declarado, el cargador lo crea automáticamente. Las extensiones aceptadas son \`.json\` y \`.csv\`.

## Compilar, probar y ejecutar

Requisitos: JDK 17 o superior y Maven 3.8 o superior. Extrae el ZIP en la raíz del repositorio y ejecuta desde la carpeta creada:

~~~powershell
Expand-Archive -LiteralPath .\red-colaboradores.zip -DestinationPath .
Set-Location .\red-colaboradores
mvn clean test
mvn clean package
java -cp target/classes consultora.red.Main data/red.json -o resultados.txt --par Hugo Ana --par Irene Fabián --par Paula Ana
~~~

El argumento de archivo puede ser JSON o CSV. \`-o\` cambia el archivo de salida y \`--par ORIGEN DESTINO\` puede repetirse para solicitar varios caminos. Si no se indica un archivo de entrada, se usa \`data/red.json\`; el archivo de salida predeterminado es \`resultados.txt\`.

## Pruebas y casos límite

La versión del ZIP tiene **74 pruebas JUnit 5**. Se ejecutó \`mvn clean test\`: 74 pruebas aprobadas, 0 fallidas y 0 errores. La suite cubre los seis casos exigidos en el examen: grafo con ciclo, árbol, dos componentes, un nodo, grafo vacío y auto-lazo.

También valida lectura JSON/CSV, aristas duplicadas, nombres con acentos, grados, ciclos, caminos y articulaciones. Compara resultados con oráculos independientes: ordenamiento de Kahn para ciclos dirigidos, búsqueda exhaustiva sin poda para caminos máximos, programación dinámica en DAG y eliminación de nodos para puntos de articulación; incluye grafos aleatorios con semilla fija y recorridos profundos.

## Resultado de ejemplo

El archivo [resultados.txt](resultados.txt) corresponde a la red de ejemplo de 16 nodos y 15 aristas dirigidas: reporta tres componentes débiles, identifica ciclos en ambas interpretaciones y enumera siete puntos de articulación. Los pares de consulta y sus caminos aparecen en tablas, junto con grados y conteo de expansiones del backtracking.

## Implementación anterior en la raíz

La carpeta \`src/main/java/mx/edu/estructuras/red/\` y sus pruebas en \`src/test/java/\` pertenecen a la implementación anterior en Java que ya estaba en el repositorio. Su archivo CSV independiente es \`red.csv\`. El paquete completo y los algoritmos descritos en las secciones anteriores se encuentran dentro de \`red-colaboradores.zip\`.

## Referencias algorítmicas

- Cormen, Leiserson, Rivest y Stein. *Introduction to Algorithms*, 4.ª ed., MIT Press, 2022. Recorridos BFS/DFS y grafos.
- R. E. Tarjan. “Depth-first search and linear graph algorithms”. *SIAM Journal on Computing*, 1(2), 1972.
- J. Hopcroft y R. Tarjan. “Algorithm 447: Efficient Algorithms for Graph Manipulation”. *Communications of the ACM*, 16(6), 1973.
- M. R. Garey y D. S. Johnson. *Computers and Intractability*, 1979. Complejidad de problemas de caminos.
- A. B. Kahn. “Topological Sorting of Large Networks”. *Communications of the ACM*, 5(11), 1962. Referencia para la validación de ciclos y DAG.
