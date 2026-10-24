package mx.edu.estructuras.red;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraphAnalyzerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsCycleInDirectedAndUndirectedGraphs() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "A");

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        assertTrue(analyzer.hasDirectedCycle());
        assertTrue(analyzer.hasUndirectedCycle());
        assertEquals(List.of("A", "B", "C"), analyzer.connectedComponentsBfs().get(0));
    }

    @Test
    void treeHasNoCycleAndReturnsItsArticulationPoint() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("B", "D");

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        assertFalse(analyzer.hasDirectedCycle());
        assertFalse(analyzer.hasUndirectedCycle());
        assertEquals(Set.of("B"), analyzer.articulationPoints());
    }

    @Test
    void findsExactlyTwoWeakComponentsWithBothTraversals() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("C", "D");

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        List<List<String>> expected = List.of(List.of("A", "B"), List.of("C", "D"));
        assertEquals(expected, analyzer.connectedComponentsDfs());
        assertEquals(expected, analyzer.connectedComponentsBfs());
    }

    @Test
    void handlesSingleVertexAndZeroLengthPath() {
        Graph graph = new Graph();
        graph.addVertex("Solo");

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        assertEquals(List.of(List.of("Solo")), analyzer.connectedComponentsDfs());
        assertFalse(analyzer.hasDirectedCycle());
        assertFalse(analyzer.hasUndirectedCycle());
        assertEquals(List.of("Solo"), analyzer.longestSimplePath("Solo", "Solo"));
        assertEquals(Map.of("Solo", 0), analyzer.inDegrees());
        assertEquals(Map.of("Solo", 0), analyzer.outDegrees());
    }

    @Test
    void handlesEmptyNetwork() {
        GraphAnalyzer analyzer = new GraphAnalyzer(new Graph());
        assertEquals(List.of(), analyzer.connectedComponentsDfs());
        assertEquals(List.of(), analyzer.connectedComponentsBfs());
        assertFalse(analyzer.hasDirectedCycle());
        assertFalse(analyzer.hasUndirectedCycle());
        assertEquals(List.of(), analyzer.longestSimplePath("A", "B"));
        assertEquals(Set.of(), analyzer.articulationPoints());
    }

    @Test
    void selfLoopIsACycleAndContributesToBothDegrees() {
        Graph graph = new Graph();
        graph.addEdge("A", "A");

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        assertTrue(analyzer.hasDirectedCycle());
        assertTrue(analyzer.hasUndirectedCycle());
        assertEquals(Map.of("A", 1), analyzer.inDegrees());
        assertEquals(Map.of("A", 1), analyzer.outDegrees());
        assertTrue(analyzer.articulationPoints().isEmpty());
    }

    @Test
    void findsLongestSimpleDirectedPathAndCorrectDegrees() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "C");
        graph.addEdge("C", "D");
        graph.addEdge("B", "D");
        graph.addVertex("Z");

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        assertEquals(List.of("A", "B", "C", "D"), analyzer.longestSimplePath("A", "D"));
        assertEquals(Map.of("A", 0, "B", 1, "C", 2, "D", 2, "Z", 0), analyzer.inDegrees());
        assertEquals(Map.of("A", 2, "B", 2, "C", 1, "D", 0, "Z", 0), analyzer.outDegrees());
        assertTrue(analyzer.longestSimplePath("Z", "A").isEmpty());
    }

    @Test
    void readsCsvWithIsolatedVerticesAndQuotedCommas() throws IOException {
        Path csv = temporaryDirectory.resolve("red.csv");
        Files.writeString(csv, "# comentario antes del encabezado\n"
                + "tipo,origen,destino\n"
                + "nodo,\"A, B\"\n"
                + "nodo,Isla\n"
                + "arista,\"A, B\",C\n");

        Graph graph = CsvGraphReader.read(csv);
        assertEquals(Set.of("A, B", "C", "Isla"), graph.vertices());
        assertEquals(2, new GraphAnalyzer(graph).connectedComponentsBfs().size());
    }
}
