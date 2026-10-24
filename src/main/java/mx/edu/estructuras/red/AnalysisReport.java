package mx.edu.estructuras.red;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Builds the tabulated console/file output requested by the exam. */
public final class AnalysisReport {
    private AnalysisReport() {
    }

    public static String render(Graph graph, String inputName, String start, String end) {
        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        List<List<String>> components = analyzer.connectedComponentsDfs();
        Map<String, Integer> inDegrees = analyzer.inDegrees();
        Map<String, Integer> outDegrees = analyzer.outDegrees();
        List<String> path = analyzer.longestSimplePath(start, end);
        List<String> articulation = new ArrayList<>(analyzer.articulationPoints());
        Collections.sort(articulation);

        StringBuilder report = new StringBuilder();
        report.append("ANALISIS DE RED DE COLABORADORES\n");
        report.append("================================\n");
        report.append("Archivo de entrada\t").append(inputName).append('\n');
        report.append("Vertices\t").append(graph.vertices().size()).append('\n');
        report.append("Aristas dirigidas\t").append(graph.edgeCount()).append('\n');
        report.append("Componentes conexas debiles\t").append(components.size()).append('\n');
        for (int index = 0; index < components.size(); index++) {
            report.append("  Componente ").append(index + 1).append('\t')
                    .append(String.join(", ", components.get(index))).append('\n');
        }
        report.append("Ciclo dirigido\t").append(yesNo(analyzer.hasDirectedCycle())).append('\n');
        report.append("Ciclo no dirigido\t").append(yesNo(analyzer.hasUndirectedCycle())).append('\n');
        report.append('\n').append("Grado de influencia (salida = relaciones iniciadas; entrada = relaciones recibidas)\n");
        report.append("Colaborador\tEntrada\tSalida\n");
        for (String vertex : graph.sortedVertices()) {
            report.append(vertex).append('\t').append(inDegrees.get(vertex)).append('\t')
                    .append(outDegrees.get(vertex)).append('\n');
        }
        report.append('\n').append("Camino simple mas largo dirigido\t")
                .append(start).append(" -> ").append(end).append('\n');
        if (path.isEmpty()) {
            report.append("  Sin camino disponible\n");
        } else {
            report.append("  Aristas\t").append(path.size() - 1).append('\n');
            report.append("  Ruta\t").append(String.join(" -> ", path)).append('\n');
        }
        report.append('\n').append("Puntos de articulacion (grafo no dirigido)\t");
        report.append(articulation.isEmpty() ? "Ninguno" : String.join(", ", articulation)).append('\n');
        report.append('\n').append("Complejidad\n");
        report.append("  Componentes y ciclos\tO(V + E)\n");
        report.append("  Grados\tO(V + E)\n");
        report.append("  Articulaciones (Tarjan)\tO(V + E)\n");
        report.append("  Camino simple mas largo\tExponencial en el peor caso; O(V!) como cota general\n");
        report.append("  Poda\tSe descarta una rama si aristas actuales + vertices no visitados <= mejor longitud\n");
        report.append("\nNota: la ruta mas larga simple es NP-dificil en general; los pesos son unitarios.\n");
        return report.toString();
    }

    private static String yesNo(boolean value) {
        return value ? "SI" : "NO";
    }
}
