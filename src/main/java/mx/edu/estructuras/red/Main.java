package mx.edu.estructuras.red;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Command-line entry point for the collaborator network analyzer. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path input = args.length > 0 ? Path.of(args[0]) : Path.of("red.csv");
        Path output = args.length > 1 ? Path.of(args[1]) : Path.of("resultados.txt");
        Graph graph = CsvGraphReader.read(input);
        List<String> vertices = graph.sortedVertices();
        String start = args.length > 2 ? args[2] : (vertices.isEmpty() ? "-" : vertices.get(0));
        String end = args.length > 3 ? args[3] : (vertices.isEmpty() ? "-" : vertices.get(vertices.size() - 1));

        String report = AnalysisReport.render(graph, input.toString(), start, end);
        Path outputParent = output.toAbsolutePath().getParent();
        if (outputParent != null) {
            Files.createDirectories(outputParent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
        System.out.print(report);
        System.out.println("Resultados exportados a " + output.toAbsolutePath());
    }
}
