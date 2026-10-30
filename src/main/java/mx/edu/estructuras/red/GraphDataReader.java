package mx.edu.estructuras.red;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads the exam's CSV format or the documented JSON graph schema. */
public final class GraphDataReader {
    private GraphDataReader() {
    }

    public static Graph read(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            return CsvGraphReader.read(path);
        }
        if (!name.endsWith(".json")) {
            throw new IOException("Formato no admitido. Selecciona un archivo CSV o JSON.");
        }
        String source = Files.readString(path, StandardCharsets.UTF_8);
        try {
            Object value = new JsonParser(source).parseDocument();
            if (!(value instanceof Map<?, ?> root)) {
                throw new IllegalArgumentException("el documento debe ser un objeto JSON");
            }
            Graph graph = new Graph();
            Object nodesValue = first(root, "nodos", "vertices", "nodes");
            if (nodesValue != null) {
                if (!(nodesValue instanceof List<?> nodes)) {
                    throw new IllegalArgumentException("'nodos' debe ser una lista");
                }
                for (Object node : nodes) {
                    if (node instanceof String id) {
                        graph.addVertex(id);
                    } else if (node instanceof Map<?, ?> item) {
                        graph.addVertex(asText(first(item, "id", "identificador", "nombre"), "nodo"));
                    } else {
                        throw new IllegalArgumentException("cada nodo debe ser texto u objeto");
                    }
                }
            }
            Object edgesValue = first(root, "aristas", "edges", "relaciones");
            if (edgesValue != null) {
                if (!(edgesValue instanceof List<?> edges)) {
                    throw new IllegalArgumentException("'aristas' debe ser una lista");
                }
                for (Object edge : edges) {
                    if (edge instanceof Map<?, ?> item) {
                        String from = asText(first(item, "origen", "from", "source"), "origen");
                        String to = asText(first(item, "destino", "to", "target"), "destino");
                        graph.addEdge(from, to);
                    } else if (edge instanceof List<?> pair && pair.size() >= 2) {
                        graph.addEdge(asText(pair.get(0), "origen"), asText(pair.get(1), "destino"));
                    } else {
                        throw new IllegalArgumentException("cada arista requiere origen y destino");
                    }
                }
            }
            if (nodesValue == null && edgesValue == null) {
                throw new IllegalArgumentException("se esperaba la lista 'nodos' y/o 'aristas'");
            }
            return graph;
        } catch (IllegalArgumentException exception) {
            throw new IOException("JSON invalido en " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static Object first(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static String asText(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("'" + field + "' debe ser texto no vacio");
        }
        return text;
    }

    /** Small recursive-descent parser for JSON values used by the graph schema. */
    private static final class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input;
        }

        private Object parseDocument() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != input.length()) {
                fail("contenido despues del objeto");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                return fail("fin inesperado del documento");
            }
            return switch (input.charAt(index)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> { expectWord("true"); yield Boolean.TRUE; }
                case 'f' -> { expectWord("false"); yield Boolean.FALSE; }
                case 'n' -> { expectWord("null"); yield null; }
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (take('}')) {
                return object;
            }
            while (true) {
                skipWhitespace();
                if (index >= input.length() || input.charAt(index) != '"') {
                    fail("se esperaba una clave entre comillas");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (take('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (take(']')) {
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (take(']')) {
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return result.toString();
                }
                if (current != '\\') {
                    if (current < 0x20) {
                        fail("caracter de control en texto");
                    }
                    result.append(current);
                    continue;
                }
                if (index >= input.length()) {
                    fail("escape incompleto");
                }
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (index + 4 > input.length()) {
                            fail("escape Unicode incompleto");
                        }
                        try {
                            result.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
                        } catch (NumberFormatException exception) {
                            fail("escape Unicode invalido");
                        }
                        index += 4;
                    }
                    default -> fail("escape JSON invalido");
                }
            }
            return fail("texto sin cerrar");
        }

        private Number parseNumber() {
            int start = index;
            if (take('-')) {
                // sign
            }
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (take('.')) {
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (index < input.length() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (start == index) {
                return fail("valor JSON invalido");
            }
            try {
                return Double.parseDouble(input.substring(start, index));
            } catch (NumberFormatException exception) {
                return fail("numero JSON invalido");
            }
        }

        private void expectWord(String word) {
            if (!input.startsWith(word, index)) {
                fail("valor JSON invalido");
            }
            index += word.length();
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                fail("se esperaba '" + expected + "'");
            }
            index++;
        }

        private boolean take(char expected) {
            skipWhitespace();
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private <T> T fail(String message) {
            throw new IllegalArgumentException(message + " (posicion " + (index + 1) + ")");
        }
    }
}

