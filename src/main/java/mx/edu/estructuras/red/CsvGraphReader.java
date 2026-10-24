package mx.edu.estructuras.red;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the documented UTF-8 CSV format without third-party libraries. */
public final class CsvGraphReader {
    private CsvGraphReader() {
    }

    /**
     * Reads {@code tipo,origen,destino} records. A {@code nodo} record has two
     * fields; an {@code arista} record has three fields.
     */
    public static Graph read(Path path) throws IOException {
        Graph graph = new Graph();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean firstRecord = true;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            List<String> fields;
            try {
                fields = parseCsvLine(line);
            } catch (IllegalArgumentException exception) {
                throw formatError(path, index + 1, exception.getMessage());
            }
            if (firstRecord && !fields.isEmpty() && fields.get(0).equalsIgnoreCase("tipo")) {
                firstRecord = false;
                continue;
            }
            if (fields.isEmpty()) {
                continue;
            }
            firstRecord = false;
            String type = fields.get(0).trim().toLowerCase(Locale.ROOT);
            try {
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
            } catch (IllegalArgumentException exception) {
                throw formatError(path, index + 1, exception.getMessage());
            }
        }
        return graph;
    }

    private static void requireFieldCount(List<String> fields, int expected, String format) {
        if (fields.size() != expected) {
            throw new IllegalArgumentException("se esperaba " + format + " (" + expected + " campos)");
        }
    }

    private static IOException formatError(Path path, int line, String message) {
        return new IOException("CSV invalido en " + path + ", linea " + line + ": " + message);
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
