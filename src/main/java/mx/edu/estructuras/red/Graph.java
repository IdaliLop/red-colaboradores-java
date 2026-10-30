package mx.edu.estructuras.red;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo dirigido no ponderado y su proyeccion no dirigida.
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

    /** Removes one directed relation and updates the undirected projection. */
    public boolean removeEdge(String from, String to) {
        String source = normalizeId(from);
        String target = normalizeId(to);
        Set<String> outgoing = directedAdjacency.get(source);
        if (outgoing == null || !outgoing.remove(target)) {
            return false;
        }
        if (!directedAdjacency.getOrDefault(target, Set.of()).contains(source)) {
            undirectedAdjacency.get(source).remove(target);
            undirectedAdjacency.get(target).remove(source);
        }
        return true;
    }

    /** Removes a vertex and all relations incident to it. */
    public boolean removeVertex(String id) {
        String vertex = normalizeId(id);
        if (!directedAdjacency.containsKey(vertex)) {
            return false;
        }
        directedAdjacency.remove(vertex);
        for (Set<String> outgoing : directedAdjacency.values()) {
            outgoing.remove(vertex);
        }
        rebuildUndirectedProjection();
        return true;
    }

    private void rebuildUndirectedProjection() {
        undirectedAdjacency.clear();
        for (String vertex : directedAdjacency.keySet()) {
            undirectedAdjacency.put(vertex, new HashSet<>());
        }
        for (Map.Entry<String, Set<String>> entry : directedAdjacency.entrySet()) {
            for (String target : entry.getValue()) {
                undirectedAdjacency.get(entry.getKey()).add(target);
                undirectedAdjacency.get(target).add(entry.getKey());
            }
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

