# Red de colaboradores

Repositorio del examen práctico de Estructuras de Datos y Algoritmia. Contiene la implementación Java que ya estaba en la raíz y el paquete completo adjunto más reciente.

## Paquete adjunto

- [Descargar el ZIP original](red-colaboradores.zip). Incluye el proyecto Java 17, su README y POM, código fuente, 74 pruebas JUnit 5 (74/74 pasan con `mvn clean test`), reporte PDF, datos JSON y CSV, y archivos de apoyo. Al extraerlo, el proyecto queda en la carpeta `red-colaboradores/`; sigue el README incluido y ejecuta `mvn clean test` desde esa carpeta.
- [CSV de la red adjunta](data/red.csv). Es la copia extraída del ZIP y representa la misma red que `red-colaboradores/data/red.json`, incluida dentro del paquete.
- [Resultados adjuntos](resultados.txt). Corresponden a la red de 16 nodos y 15 aristas de `data/red.json` dentro del ZIP.

## Implementación Java disponible en la raíz

La implementación que ya estaba en el repositorio permanece en `src/main/java/mx/edu/estructuras/red/`, con sus pruebas en `src/test/java/`. Usa Java 17, Maven y CSV. Su archivo de ejemplo independiente es `red.csv` (14 nodos); para ejecutar sus pruebas desde la raíz:

```bash
mvn test
```

El archivo `resultados.txt` de la raíz es el resultado adjunto del paquete ZIP. La implementación Java de la raíz puede regenerar su salida con:

```bash
java -cp target/classes mx.edu.estructuras.red.Main red.csv resultados.txt Ada Kira
```
