package mx.edu.estructuras.red;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads both documented typed CSV records and ordinary origin,destination CSV files. */
public final class CsvGraphReader {
    private enum Format { TYPED, PAIRS }

    private CsvGraphReader() {
    }

    /**
     * Reads either tipo,nodo / tipo,arista records or one directed relation per
     * origen,destino row. A blank destination or a single value declares an
     * isolated vertex.
     */
    public static Graph read(Path path) throws IOException {
        Graph graph = new Graph();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Format format = null;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == 0) {
                line = line.replace("\uFEFF", "");
            }
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            List<String> fields;
            try {
                fields = parseCsvLine(line);
            } catch (IllegalArgumentException exception) {
                throw formatError(path, index + 1, exception.getMessage());
            }
            if (fields.isEmpty()) {
                continue;
            }

            String first = normalized(fields.get(0));
            if (format == null) {
                if (first.equals("tipo")) {
                    format = Format.TYPED;
                    continue;
                }
                if (isPairHeader(fields)) {
                    format = Format.PAIRS;
                    continue;
                }
                format = first.equals("nodo") || first.equals("arista")
                        ? Format.TYPED : Format.PAIRS;
            } else if (isPairHeader(fields)) {
                // Some exported files repeat the column header after metadata.
                continue;
            }

            try {
                if (format == Format.TYPED) {
                    readTypedRecord(graph, fields);
                } else {
                    readPairRecord(graph, fields);
                }
            } catch (IllegalArgumentException exception) {
                throw formatError(path, index + 1, exception.getMessage());
            }
        }
        return graph;
    }

    private static void readTypedRecord(Graph graph, List<String> fields) {
        String type = normalized(fields.get(0));
        switch (type) {
            case "nodo" -> {
                requireFieldCount(fields, 2, "nodo,identificador");
                graph.addVertex(fields.get(1));
            }
            case "arista" -> {
                requireFieldCount(fields, 3, "arista,origen,destino");
                graph.addEdge(fields.get(1), fields.get(2));
            }
            default -> throw new IllegalArgumentException("tipo desconocido: " + fields.get(0));
        }
    }

    private static void readPairRecord(Graph graph, List<String> fields) {
        if (fields.size() == 1) {
            graph.addVertex(fields.get(0));
            return;
        }
        requireFieldCount(fields, 2, "origen,destino");
        String from = fields.get(0);
        String to = fields.get(1);
        if (to.isBlank()) {
            graph.addVertex(from);
        } else {
            graph.addEdge(from, to);
        }
    }

    private static boolean isPairHeader(List<String> fields) {
        if (fields.size() < 2) {
            return false;
        }
        String first = normalized(fields.get(0));
        String second = normalized(fields.get(1));
        boolean source = first.equals("origen") || first.equals("source")
                || first.equals("from") || first.equals("inicio");
        boolean target = second.equals("destino") || second.equals("target")
                || second.equals("to") || second.equals("fin");
        return source && target;
    }

    private static String normalized(String value) {
        return value.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
    }

    private static void requireFieldCount(List<String> fields, int expected, String format) {
        if (fields.size() != expected) {
            throw new IllegalArgumentException("se esperaba " + format + " (" + expected + " campos)");
        }
    }

    private static IOException formatError(Path path, int line, String message) {
        return new IOException("CSV inválido en " + path + ", línea " + line + ": " + message);
    }

    /** Parses one CSV row, including quoted commas and escaped double quotes. */
    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("campo entre comillas sin cerrar");
        }
        fields.add(field.toString().trim());
        return fields;
    }
}
