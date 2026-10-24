package mx.edu.estructuras.red;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Implementa los recorridos y metricas requeridos sobre {@link Graph}. */
public final class GraphAnalyzer {
    private enum VisitState { GRAY, BLACK }

    private final Graph graph;

    public GraphAnalyzer(Graph graph) {
        this.graph = graph;
    }

    /** Finds weakly connected components using iterative depth-first search. */
    public List<List<String>> connectedComponentsDfs() {
        Set<String> visited = new HashSet<>();
        List<List<String>> components = new ArrayList<>();
        for (String start : graph.sortedVertices()) {
            if (!visited.add(start)) {
                continue;
            }
            List<String> component = new ArrayList<>();
            ArrayDeque<String> stack = new ArrayDeque<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                String current = stack.pop();
                component.add(current);
                List<String> neighbors = sorted(graph.undirectedNeighbors(current));
                Collections.reverse(neighbors);
                for (String neighbor : neighbors) {
                    if (visited.add(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
            Collections.sort(component);
            components.add(component);
        }
        return immutableComponents(components);
    }

    /** Finds the same weakly connected components using breadth-first search. */
    public List<List<String>> connectedComponentsBfs() {
        Set<String> visited = new HashSet<>();
        List<List<String>> components = new ArrayList<>();
        for (String start : graph.sortedVertices()) {
            if (!visited.add(start)) {
                continue;
            }
            List<String> component = new ArrayList<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                String current = queue.remove();
                component.add(current);
                for (String neighbor : sorted(graph.undirectedNeighbors(current))) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            Collections.sort(component);
            components.add(component);
        }
        return immutableComponents(components);
    }

    /** Detects a directed cycle by iterative DFS and three-color state. */
    public boolean hasDirectedCycle() {
        Map<String, VisitState> states = new HashMap<>();
        for (String start : graph.sortedVertices()) {
            if (states.containsKey(start)) {
                continue;
            }
            states.put(start, VisitState.GRAY);
            ArrayDeque<DfsFrame> stack = new ArrayDeque<>();
            stack.push(frame(start));
            while (!stack.isEmpty()) {
                DfsFrame top = stack.peek();
                if (!top.neighbors().hasNext()) {
                    states.put(top.vertex(), VisitState.BLACK);
                    stack.pop();
                    continue;
                }
                String neighbor = top.neighbors().next();
                VisitState state = states.get(neighbor);
                if (state == VisitState.GRAY) {
                    return true;
                }
                if (state == null) {
                    states.put(neighbor, VisitState.GRAY);
                    stack.push(frame(neighbor));
                }
            }
        }
        return false;
    }

    /** Detects an undirected cycle in the projection, including a self-loop. */
    public boolean hasUndirectedCycle() {
        Set<String> visited = new HashSet<>();
        Map<String, String> parent = new HashMap<>();
        for (String start : graph.sortedVertices()) {
            if (!visited.add(start)) {
                continue;
            }
            parent.put(start, null);
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                String current = queue.remove();
                for (String neighbor : sorted(graph.undirectedNeighbors(current))) {
                    if (visited.add(neighbor)) {
                        parent.put(neighbor, current);
                        queue.add(neighbor);
                    } else if (!neighbor.equals(parent.get(current))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Returns every vertex's incoming-edge count. */
    public Map<String, Integer> inDegrees() {
        Map<String, Integer> result = zeroDegrees();
        for (String source : graph.sortedVertices()) {
            for (String target : graph.directedNeighbors(source)) {
                result.merge(target, 1, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Returns every vertex's outgoing-edge count. */
    public Map<String, Integer> outDegrees() {
        Map<String, Integer> result = zeroDegrees();
        for (String vertex : graph.sortedVertices()) {
            result.put(vertex, graph.directedNeighbors(vertex).size());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Searches for a longest simple directed path from {@code start} to
     * {@code end}. Branch-and-bound prunes when remaining vertices cannot
     * improve the best path found so far.
     */
    public List<String> longestSimplePath(String start, String end) {
        if (!graph.vertices().contains(start) || !graph.vertices().contains(end)) {
            return List.of();
        }
        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();
        List<String> best = new ArrayList<>();
        visited.add(start);
        path.add(start);
        searchLongestPath(start, end, visited, path, best);
        return List.copyOf(best);
    }

    /** Finds articulation points using Tarjan's discovery and low-link values. */
    public Set<String> articulationPoints() {
        Map<String, Integer> discovered = new HashMap<>();
        Map<String, Integer> low = new HashMap<>();
        Set<String> articulationPoints = new HashSet<>();
        int[] time = {0};
        for (String vertex : graph.sortedVertices()) {
            if (!discovered.containsKey(vertex)) {
                articulationDfs(vertex, null, discovered, low, articulationPoints, time);
            }
        }
        return Collections.unmodifiableSet(articulationPoints);
    }

    private void searchLongestPath(String current, String end, Set<String> visited,
                                   List<String> path, List<String> best) {
        if (current.equals(end)) {
            if (path.size() > best.size()) {
                best.clear();
                best.addAll(path);
            }
            return;
        }
        int currentEdges = path.size() - 1;
        int remainingVertices = graph.vertices().size() - visited.size();
        int upperBoundEdges = currentEdges + remainingVertices;
        int bestEdges = best.isEmpty() ? -1 : best.size() - 1;
        if (upperBoundEdges <= bestEdges) {
            return;
        }
        for (String neighbor : sorted(graph.directedNeighbors(current))) {
            if (visited.add(neighbor)) {
                path.add(neighbor);
                searchLongestPath(neighbor, end, visited, path, best);
                path.remove(path.size() - 1);
                visited.remove(neighbor);
            }
        }
    }

    private void articulationDfs(String vertex, String parent, Map<String, Integer> discovered,
                                 Map<String, Integer> low, Set<String> result, int[] time) {
        int discoveryTime = ++time[0];
        discovered.put(vertex, discoveryTime);
        low.put(vertex, discoveryTime);
        int children = 0;
        for (String neighbor : sorted(graph.undirectedNeighbors(vertex))) {
            if (!discovered.containsKey(neighbor)) {
                children++;
                articulationDfs(neighbor, vertex, discovered, low, result, time);
                low.put(vertex, Math.min(low.get(vertex), low.get(neighbor)));
                if (parent == null && children > 1) {
                    result.add(vertex);
                }
                if (parent != null && low.get(neighbor) >= discovered.get(vertex)) {
                    result.add(vertex);
                }
            } else if (!neighbor.equals(parent)) {
                low.put(vertex, Math.min(low.get(vertex), discovered.get(neighbor)));
            }
        }
    }

    private Map<String, Integer> zeroDegrees() {
        Map<String, Integer> degrees = new HashMap<>();
        for (String vertex : graph.sortedVertices()) {
            degrees.put(vertex, 0);
        }
        return degrees;
    }

    private DfsFrame frame(String vertex) {
        return new DfsFrame(vertex, sorted(graph.directedNeighbors(vertex)).iterator());
    }

    private static List<String> sorted(Set<String> values) {
        List<String> result = new ArrayList<>(values);
        Collections.sort(result);
        return result;
    }

    private static List<List<String>> immutableComponents(List<List<String>> components) {
        List<List<String>> immutable = new ArrayList<>();
        for (List<String> component : components) {
            immutable.add(List.copyOf(component));
        }
        return List.copyOf(immutable);
    }

    private record DfsFrame(String vertex, Iterator<String> neighbors) {
    }
}
