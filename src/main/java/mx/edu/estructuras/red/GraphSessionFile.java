package mx.edu.estructuras.red;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formato de sesion de texto versionado. No utiliza la serializacion de Java.
 * Las etiquetas se codifican en Base64 UTF-8 para permitir cualquier texto.
 */
public final class GraphSessionFile {
    private static final String HEADER = "RED-COLABORADORES\t1";

    private GraphSessionFile() {
    }

    public static void save(Path path, Graph graph, Map<String, Point2D.Double> positions,
                            boolean darkTheme, String start, String end) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add("THEME\t" + (darkTheme ? "DARK" : "LIGHT"));
        lines.add("PATH\t" + encode(nullToEmpty(start)) + "\t" + encode(nullToEmpty(end)));
        for (String vertex : graph.sortedVertices()) {
            Point2D.Double point = positions.getOrDefault(vertex, new Point2D.Double(100, 100));
            lines.add("NODE\t" + encode(vertex) + "\t" + point.x + "\t" + point.y);
        }
        for (String source : graph.sortedVertices()) {
            for (String target : graph.directedNeighbors(source)) {
                lines.add("EDGE\t" + encode(source) + "\t" + encode(target));
            }
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    public static Snapshot load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            throw new IOException("La sesion no tiene una cabecera reconocida.");
        }
        Graph graph = new Graph();
        Map<String, Point2D.Double> positions = new LinkedHashMap<>();
        boolean darkTheme = false;
        String start = "";
        String end = "";
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            try {
                switch (fields[0]) {
                    case "THEME" -> {
                        requireLength(fields, 2);
                        if (!fields[1].equals("DARK") && !fields[1].equals("LIGHT")) {
                            throw new IllegalArgumentException("tema desconocido");
                        }
                        darkTheme = fields[1].equals("DARK");
                    }
                    case "PATH" -> {
                        requireLength(fields, 3);
                        start = decode(fields[1]);
                        end = decode(fields[2]);
                    }
                    case "NODE" -> {
                        requireLength(fields, 4);
                        String id = decode(fields[1]);
                        graph.addVertex(id);
                        positions.put(id, new Point2D.Double(
                                Double.parseDouble(fields[2]), Double.parseDouble(fields[3])));
                    }
                    case "EDGE" -> {
                        requireLength(fields, 3);
                        graph.addEdge(decode(fields[1]), decode(fields[2]));
                    }
                    default -> throw new IllegalArgumentException("registro desconocido: " + fields[0]);
                }
            } catch (RuntimeException exception) {
                throw new IOException("Sesion invalida en linea " + (i + 1) + ": "
                        + exception.getMessage(), exception);
            }
        }
        return new Snapshot(graph, positions, darkTheme, start, end);
    }

    private static void requireLength(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("numero de campos incorrecto");
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record Snapshot(Graph graph, Map<String, Point2D.Double> positions,
                           boolean darkTheme, String start, String end) {
    }
}

