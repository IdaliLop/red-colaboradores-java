package mx.edu.estructuras.red;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo dirigido ponderado con peso unitario y su proyeccion no dirigida.
 * Los identificadores se conservan literalmente despues de quitar espacios
 * externos. Las aristas duplicadas se consideran una sola relacion.
 */
public final class Graph {
    private final Map<String, Set<String>> directedAdjacency = new HashMap<>();
    private final Map<String, Set<String>> undirectedAdjacency = new HashMap<>();

    /** Adds an isolated vertex if it is not already present. */
    public void addVertex(String id) {
        String vertex = normalizeId(id);
        directedAdjacency.computeIfAbsent(vertex, ignored -> new HashSet<>());
        undirectedAdjacency.computeIfAbsent(vertex, ignored -> new HashSet<>());
    }

    /** Adds the directed relation {@code from -> to} and its undirected projection. */
    public void addEdge(String from, String to) {
        String source = normalizeId(from);
        String target = normalizeId(to);
        addVertex(source);
        addVertex(target);
        if (directedAdjacency.get(source).add(target)) {
            undirectedAdjacency.get(source).add(target);
            undirectedAdjacency.get(target).add(source);
        }
    }

    /** Returns all vertex identifiers. */
    public Set<String> vertices() {
        return Collections.unmodifiableSet(directedAdjacency.keySet());
    }

    /** Returns the directed outgoing neighbors of a vertex. */
    public Set<String> directedNeighbors(String vertex) {
        return immutableCopy(directedAdjacency.get(vertex));
    }

    /** Returns the neighbors in the undirected projection. */
    public Set<String> undirectedNeighbors(String vertex) {
        return immutableCopy(undirectedAdjacency.get(vertex));
    }

    /** Returns vertices in deterministic alphabetical order. */
    public List<String> sortedVertices() {
        List<String> vertices = new ArrayList<>(directedAdjacency.keySet());
        Collections.sort(vertices);
        return vertices;
    }

    /** Returns the number of unique directed edges. */
    public int edgeCount() {
        return directedAdjacency.values().stream().mapToInt(Set::size).sum();
    }

    private static Set<String> immutableCopy(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new HashSet<>(values));
    }

    private static String normalizeId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador del colaborador no puede estar vacio.");
        }
        return id.trim();
    }
}
