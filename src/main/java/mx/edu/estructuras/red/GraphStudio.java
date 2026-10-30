package mx.edu.estructuras.red;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Interactive Swing workspace for exploring the collaborator network. */
public final class GraphStudio extends JFrame {
    private Graph graph = new Graph();
    private final GraphCanvas canvas = new GraphCanvas();
    private final JLabel statusLabel = new JLabel("Listo");
    private final JLabel statsLabel = new JLabel("Vértices: 0  ·  Aristas: 0");
    private final JLabel fileLabel = new JLabel("Sin archivo");
    private final JTextArea resultsArea = new JTextArea();
    private final JTextField newVertexField = new JTextField();
    private final JComboBox<String> edgeFrom = new JComboBox<>();
    private final JComboBox<String> edgeTo = new JComboBox<>();
    private final JComboBox<String> pathStart = new JComboBox<>();
    private final JComboBox<String> pathEnd = new JComboBox<>();
    private final JButton analyzeButton = new JButton("Analizar red");
    private final JButton themeButton = new JButton("☾ Tema oscuro");
    private boolean darkTheme;
    private Path currentFile;

    private GraphStudio() {
        super("Red de colaboradores · Graph Studio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1460, 900);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        canvas.setGraph(graph, Map.of());
        installDeleteShortcut();
        applyTheme();
        loadSampleGraph();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep Swing's default look and feel if the platform theme is unavailable.
            }
            new GraphStudio().setVisible(true);
        });
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.add(buildToolbar(), BorderLayout.NORTH);

        JScrollPane graphScroll = new JScrollPane(canvas);
        graphScroll.setBorder(BorderFactory.createEmptyBorder());
        graphScroll.getViewport().setBackground(Color.WHITE);

        JTabbedPane sidePanel = new JTabbedPane();
        sidePanel.addTab("Edición", buildEditor());
        sidePanel.addTab("Resultados", buildResults());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphScroll, sidePanel);
        split.setResizeWeight(0.76);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.74));
        split.setOneTouchExpandable(true);
        root.add(split, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("RED DE COLABORADORES");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Editor visual de grafos dirigidos");
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        titleBlock.add(title);
        titleBlock.add(subtitle);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        JButton openData = new JButton("Cargar datos");
        JButton openSession = new JButton("Abrir sesión");
        JButton saveSession = new JButton("Guardar sesión");
        JButton exportPng = new JButton("Exportar PNG");
        JButton relayout = new JButton("Reordenar");
        openData.addActionListener(event -> chooseDataFile());
        openSession.addActionListener(event -> chooseSessionFile());
        saveSession.addActionListener(event -> saveSession());
        exportPng.addActionListener(event -> exportPng());
        relayout.addActionListener(event -> {
            canvas.autoLayout();
            statusLabel.setText("Vista reorganizada · guarda la sesión para conservar las posiciones.");
        });
        themeButton.addActionListener(event -> {
            darkTheme = !darkTheme;
            themeButton.setText(darkTheme ? "☀ Tema claro" : "☾ Tema oscuro");
            applyTheme();
        });
        for (JButton button : List.of(openData, openSession, saveSession, exportPng, relayout, themeButton)) {
            button.setFocusPainted(false);
            actions.add(button);
            actions.add(Box.createHorizontalStrut(7));
        }
        bar.add(titleBlock, BorderLayout.WEST);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildEditor() {
        JPanel editor = new JPanel();
        editor.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        editor.setLayout(new BoxLayout(editor, BoxLayout.Y_AXIS));

        JPanel vertexGroup = group("Colaboradores");
        newVertexField.setToolTipText("Nombre del nuevo colaborador");
        newVertexField.addActionListener(event -> addVertex());
        JButton addVertex = new JButton("Agregar");
        JButton removeVertex = new JButton("Eliminar seleccionado");
        addVertex.addActionListener(event -> addVertex());
        removeVertex.addActionListener(event -> removeSelectedVertex());
        vertexGroup.add(row(new JLabel("Nombre"), newVertexField));
        vertexGroup.add(Box.createVerticalStrut(7));
        vertexGroup.add(row(addVertex, removeVertex));

        JPanel edgeGroup = group("Relaciones dirigidas");
        edgeFrom.setToolTipText("Quien reporta");
        edgeTo.setToolTipText("A quien reporta");
        JButton addEdge = new JButton("Agregar relación");
        JButton removeEdge = new JButton("Quitar relación");
        addEdge.addActionListener(event -> addEdge());
        removeEdge.addActionListener(event -> removeEdge());
        edgeGroup.add(row(new JLabel("Origen"), edgeFrom));
        edgeGroup.add(Box.createVerticalStrut(5));
        edgeGroup.add(row(new JLabel("Destino"), edgeTo));
        edgeGroup.add(Box.createVerticalStrut(7));
        edgeGroup.add(row(addEdge, removeEdge));

        JPanel analysisGroup = group("Algoritmos del examen");
        pathStart.setToolTipText("Inicio del camino");
        pathEnd.setToolTipText("Destino del camino");
        analyzeButton.addActionListener(event -> analyzeGraph());
        analysisGroup.add(row(new JLabel("Inicio"), pathStart));
        analysisGroup.add(Box.createVerticalStrut(5));
        analysisGroup.add(row(new JLabel("Destino"), pathEnd));
        analysisGroup.add(Box.createVerticalStrut(8));
        analyzeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        analysisGroup.add(analyzeButton);
        JLabel hint = new JLabel("<html>Componentes, ciclos, grados, camino simple<br>más largo y puntos de articulación.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setBorder(BorderFactory.createEmptyBorder(9, 0, 0, 0));
        analysisGroup.add(hint);

        JPanel helpGroup = group("Interacción del lienzo");
        JLabel help = new JLabel("<html>Arrastra los nodos para moverlos.<br>Doble clic en el lienzo para crear uno.<br>Supr elimina el nodo seleccionado.</html>");
        help.setFont(help.getFont().deriveFont(11f));
        helpGroup.add(help);

        for (JPanel group : List.of(vertexGroup, edgeGroup, analysisGroup, helpGroup)) {
            group.setAlignmentX(Component.LEFT_ALIGNMENT);
            editor.add(group);
            editor.add(Box.createVerticalStrut(12));
        }
        editor.add(Box.createVerticalGlue());
        return editor;
    }

    private JPanel buildResults() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        resultsArea.setEditable(false);
        resultsArea.setLineWrap(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultsArea.setText("Carga o edita una red y pulsa “Analizar red”.\n");
        JScrollPane scroll = new JScrollPane(resultsArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 216, 224)));
        panel.add(scroll, BorderLayout.CENTER);
        JButton copyText = new JButton("Copiar resultados");
        copyText.addActionListener(event -> {
            resultsArea.selectAll();
            resultsArea.copy();
            resultsArea.select(0, 0);
            statusLabel.setText("Resultados copiados al portapapeles.");
        });
        panel.add(copyText, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        bar.add(statusLabel, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(14, 0));
        right.add(statsLabel, BorderLayout.WEST);
        right.add(fileLabel, BorderLayout.EAST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel group(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(8, 8, 9, 8)));
        return panel;
    }

    private JPanel row(Component left, Component right) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        left.setPreferredSize(new Dimension(70, 28));
        right.setPreferredSize(new Dimension(170, 28));
        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void chooseDataFile() {
        JFileChooser chooser = fileChooser("Datos del grafo", "CSV", "csv", "JSON", "json");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        try {
            Graph loaded = GraphDataReader.read(selected);
            installGraph(loaded, Map.of());
            currentFile = selected;
            fileLabel.setText(selected.getFileName().toString());
            statusLabel.setText("Datos cargados desde " + selected.toAbsolutePath());
        } catch (IOException | RuntimeException exception) {
            showError("No se pudo cargar el grafo", exception);
        }
    }

    private void chooseSessionFile() {
        JFileChooser chooser = fileChooser("Sesión de grafo", "Sesión RED", "redsession");
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        try {
            GraphSessionFile.Snapshot snapshot = GraphSessionFile.load(selected);
            installGraph(snapshot.graph(), snapshot.positions());
            darkTheme = snapshot.darkTheme();
            themeButton.setText(darkTheme ? "☀ Tema claro" : "☾ Tema oscuro");
            refreshSelectors(snapshot.start(), snapshot.end());
            applyTheme();
            currentFile = selected;
            fileLabel.setText(selected.getFileName().toString());
            statusLabel.setText("Sesión restaurada desde " + selected.toAbsolutePath());
        } catch (IOException | RuntimeException exception) {
            showError("No se pudo abrir la sesión", exception);
        }
    }

    private void saveSession() {
        JFileChooser chooser = fileChooser("Guardar sesión de grafo", "Sesión RED", "redsession");
        chooser.setSelectedFile(new java.io.File("red-colaboradores.redsession"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = withExtension(chooser.getSelectedFile().toPath(), ".redsession");
        try {
            GraphSessionFile.save(selected, graph, canvas.positions(),
                    darkTheme, selectedValue(pathStart), selectedValue(pathEnd));
            currentFile = selected;
            fileLabel.setText(selected.getFileName().toString());
            statusLabel.setText("Sesión guardada · " + selected.toAbsolutePath());
        } catch (IOException | RuntimeException exception) {
            showError("No se pudo guardar la sesión", exception);
        }
    }

    private void exportPng() {
        JFileChooser chooser = fileChooser("Exportar lienzo", "Imagen PNG", "png");
        chooser.setSelectedFile(new java.io.File("red-colaboradores.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = withExtension(chooser.getSelectedFile().toPath(), ".png");
        try {
            Dimension preferred = canvas.getPreferredSize();
            int width = Math.max(preferred.width, canvas.getWidth());
            int height = Math.max(preferred.height, canvas.getHeight());
            canvas.setSize(width, height);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            canvas.paint(graphics);
            graphics.dispose();
            Files.createDirectories(selected.toAbsolutePath().getParent());
            ImageIO.write(image, "png", selected.toFile());
            statusLabel.setText("Lienzo exportado a " + selected.toAbsolutePath());
        } catch (IOException | RuntimeException exception) {
            showError("No se pudo exportar el PNG", exception);
        }
    }

    private JFileChooser fileChooser(String title, String... filters) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        if (filters.length >= 2) {
            for (int index = 0; index + 1 < filters.length; index += 2) {
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                        filters[index], filters[index + 1]));
            }
        }
        if (Files.isDirectory(Path.of("data"))) {
            chooser.setCurrentDirectory(Path.of("data").toFile());
        }
        return chooser;
    }

    private static Path withExtension(Path path, String extension) {
        String name = path.getFileName().toString();
        if (!name.toLowerCase().endsWith(extension)) {
            path = path.resolveSibling(name + extension);
        }
        return path;
    }

    private void loadSampleGraph() {
        for (Path sample : List.of(Path.of("data/red.csv"), Path.of("red.csv"))) {
            if (Files.isRegularFile(sample)) {
                try {
                    installGraph(GraphDataReader.read(sample), Map.of());
                    currentFile = sample;
                    fileLabel.setText(sample.toString());
                    statusLabel.setText("Red de ejemplo cargada · arrastra los nodos para explorarla.");
                    return;
                } catch (IOException ignored) {
                    // Startup continues with an empty graph if the sample file is not valid.
                }
            }
        }
        updateStats();
    }

    private void installGraph(Graph replacement, Map<String, Point2D.Double> positions) {
        graph = replacement;
        canvas.setGraph(graph, positions);
        refreshSelectors();
        resultsArea.setText("Red cargada. Pulsa “Analizar red” para ver los resultados.\n");
        updateStats();
    }

    private void refreshSelectors() {
        refreshSelectors(selectedValue(pathStart), selectedValue(pathEnd));
    }

    private void refreshSelectors(String preferredStart, String preferredEnd) {
        List<String> vertices = graph.sortedVertices();
        refill(edgeFrom, vertices, null);
        refill(edgeTo, vertices, vertices.size() > 1 ? vertices.get(1) : null);
        refill(pathStart, vertices, preferredStart);
        refill(pathEnd, vertices, preferredEnd == null || preferredEnd.isEmpty() ? last(vertices) : preferredEnd);
    }

    private static void refill(JComboBox<String> combo, List<String> options, String preferred) {
        combo.removeAllItems();
        for (String option : options) {
            combo.addItem(option);
        }
        if (preferred != null && options.contains(preferred)) {
            combo.setSelectedItem(preferred);
        }
    }

    private static String last(List<String> values) {
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private static String selectedValue(JComboBox<String> combo) {
        Object selected = combo.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void addVertex() {
        String id = newVertexField.getText().trim();
        if (id.isEmpty()) {
            statusLabel.setText("Escribe el nombre del colaborador.");
            newVertexField.requestFocusInWindow();
            return;
        }
        boolean alreadyPresent = graph.vertices().contains(id);
        graph.addVertex(id);
        newVertexField.setText("");
        canvas.syncVertices();
        refreshSelectors(id, id);
        onGraphEdited(alreadyPresent ? "El colaborador ya existía." : "Colaborador agregado: " + id);
    }

    private void addVertexAt(Point2D point) {
        String entered = JOptionPane.showInputDialog(this, "Nombre del colaborador:",
                "Agregar colaborador", JOptionPane.PLAIN_MESSAGE);
        if (entered == null || entered.isBlank()) {
            return;
        }
        String id = entered.trim();
        boolean existed = graph.vertices().contains(id);
        graph.addVertex(id);
        canvas.syncVertices();
        canvas.setPosition(id, new Point2D.Double(point.getX(), point.getY()));
        refreshSelectors(id, id);
        onGraphEdited(existed ? "El colaborador ya existía." : "Colaborador agregado: " + id);
    }

    private void removeSelectedVertex() {
        String selected = canvas.selectedVertex();
        if (selected == null) {
            statusLabel.setText("Selecciona un nodo en el lienzo antes de eliminarlo.");
            return;
        }
        graph.removeVertex(selected);
        canvas.clearSelection();
        canvas.syncVertices();
        refreshSelectors();
        onGraphEdited("Colaborador eliminado: " + selected);
    }

    private void addEdge() {
        String source = selectedValue(edgeFrom);
        String target = selectedValue(edgeTo);
        if (source.isEmpty() || target.isEmpty()) {
            statusLabel.setText("Agrega dos colaboradores antes de crear una relación.");
            return;
        }
        boolean existed = graph.directedNeighbors(source).contains(target);
        graph.addEdge(source, target);
        onGraphEdited(existed ? "La relación ya existía." : source + " → " + target + " agregado.");
    }

    private void removeEdge() {
        String source = selectedValue(edgeFrom);
        String target = selectedValue(edgeTo);
        if (source.isEmpty() || target.isEmpty()) {
            return;
        }
        if (graph.removeEdge(source, target)) {
            onGraphEdited("Relación eliminada: " + source + " → " + target);
        } else {
            statusLabel.setText("No existe esa relación dirigida.");
        }
    }

    private void onGraphEdited(String message) {
        canvas.repaint();
        updateStats();
        resultsArea.setText("La red cambió. Ejecuta de nuevo el análisis para actualizar los resultados.\n");
        statusLabel.setText(message + " · guarda la sesión para conservar los cambios.");
    }

    private void updateStats() {
        statsLabel.setText("Vértices: " + graph.vertices().size() + "  ·  Aristas: " + graph.edgeCount());
    }

    private void analyzeGraph() {
        if (graph.vertices().isEmpty()) {
            statusLabel.setText("Carga o agrega colaboradores antes de ejecutar los algoritmos.");
            return;
        }
        String start = selectedValue(pathStart);
        String end = selectedValue(pathEnd);
        Graph snapshot = copyGraph(graph);
        analyzeButton.setEnabled(false);
        statusLabel.setText("Analizando componentes, ciclos, grados, caminos y puntos de articulación…");
        resultsArea.setText("Análisis en curso…\n");
        new SwingWorker<AnalysisOutcome, Void>() {
            @Override
            protected AnalysisOutcome doInBackground() {
                return analyze(snapshot, start, end);
            }

            @Override
            protected void done() {
                analyzeButton.setEnabled(true);
                try {
                    AnalysisOutcome outcome = get();
                    resultsArea.setText(outcome.report());
                    resultsArea.setCaretPosition(0);
                    canvas.animatePath(outcome.highlightedPath());
                    statusLabel.setText("Análisis terminado · el camino encontrado se resaltó en el lienzo.");
                } catch (Exception exception) {
                    showError("Falló el análisis", exception);
                    statusLabel.setText("No se pudo completar el análisis.");
                }
            }
        }.execute();
    }

    private static AnalysisOutcome analyze(Graph graph, String start, String end) {
        GraphAnalyzer analyzer = new GraphAnalyzer(graph);
        List<List<String>> components = analyzer.connectedComponentsBfs();
        Map<String, Integer> in = analyzer.inDegrees();
        Map<String, Integer> out = analyzer.outDegrees();
        List<String> directedPath = analyzer.longestSimplePath(start, end);
        List<String> undirectedPath = longestUndirectedPath(graph, start, end);
        List<String> articulation = new ArrayList<>(analyzer.articulationPoints());
        Collections.sort(articulation);

        StringBuilder text = new StringBuilder();
        text.append("ANÁLISIS DE RED DE COLABORADORES\n");
        text.append("================================\n");
        text.append("Vértices: ").append(graph.vertices().size())
                .append("    Aristas dirigidas: ").append(graph.edgeCount()).append("\n\n");
        text.append("1. COMPONENTES DÉBILMENTE CONEXAS (BFS)\n");
        text.append("   Total: ").append(components.size()).append('\n');
        for (int i = 0; i < components.size(); i++) {
            text.append("   ").append(i + 1).append(". ")
                    .append(String.join(", ", components.get(i))).append('\n');
        }
        text.append("\n2. CICLOS\n");
        text.append("   Ciclo dirigido: ").append(yesNo(analyzer.hasDirectedCycle())).append('\n');
        text.append("   Ciclo no dirigido: ").append(yesNo(analyzer.hasUndirectedCycle())).append('\n');
        text.append("\n3. GRADOS DE ENTRADA Y SALIDA\n");
        text.append(String.format("%-22s %10s %10s %10s%n", "Colaborador", "Entrada", "Salida", "Total"));
        for (String vertex : graph.sortedVertices()) {
            int incoming = in.getOrDefault(vertex, 0);
            int outgoing = out.getOrDefault(vertex, 0);
            text.append(String.format("%-22s %10d %10d %10d%n",
                    vertex, incoming, outgoing, incoming + outgoing));
        }
        text.append("\n4. CAMINO SIMPLE MÁS LARGO ENTRE ")
                .append(start).append(" Y ").append(end).append('\n');
        appendPath(text, "Dirigido", directedPath);
        appendPath(text, "No dirigido", undirectedPath);
        text.append("\n5. COLABORADORES PUENTE (PUNTOS DE ARTICULACIÓN)\n");
        text.append(articulation.isEmpty() ? "   Ninguno\n"
                : "   " + String.join(", ", articulation) + "\n");
        text.append("\nLa ruta simple más larga es NP-difícil en grafos generales.\n");
        List<String> highlight = directedPath.isEmpty() ? undirectedPath : directedPath;
        return new AnalysisOutcome(text.toString(), highlight);
    }

    private static void appendPath(StringBuilder text, String label, List<String> path) {
        text.append("   ").append(label).append(": ");
        if (path.isEmpty()) {
            text.append("sin camino\n");
        } else {
            text.append(path.size() - 1).append(" aristas · ")
                    .append(String.join(" → ", path)).append('\n');
        }
    }

    private static String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }

    private static Graph copyGraph(Graph source) {
        Graph copy = new Graph();
        for (String vertex : source.sortedVertices()) {
            copy.addVertex(vertex);
        }
        for (String from : source.sortedVertices()) {
            for (String to : source.directedNeighbors(from)) {
                copy.addEdge(from, to);
            }
        }
        return copy;
    }

    private static List<String> longestUndirectedPath(Graph graph, String start, String end) {
        if (!graph.vertices().contains(start) || !graph.vertices().contains(end)) {
            return List.of();
        }
        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();
        List<String> best = new ArrayList<>();
        visited.add(start);
        path.add(start);
        searchUndirected(graph, start, end, visited, path, best);
        return List.copyOf(best);
    }

    private static void searchUndirected(Graph graph, String current, String end, Set<String> visited,
                                         List<String> path, List<String> best) {
        if (current.equals(end)) {
            if (path.size() > best.size()) {
                best.clear();
                best.addAll(path);
            }
            return;
        }
        int possibleNodes = path.size() + graph.vertices().size() - visited.size();
        if (possibleNodes <= best.size()) {
            return;
        }
        List<String> neighbors = new ArrayList<>(graph.undirectedNeighbors(current));
        Collections.sort(neighbors);
        for (String neighbor : neighbors) {
            if (visited.add(neighbor)) {
                path.add(neighbor);
                searchUndirected(graph, neighbor, end, visited, path, best);
                path.remove(path.size() - 1);
                visited.remove(neighbor);
            }
        }
    }

    private void installDeleteShortcut() {
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove-selected-node");
        canvas.getActionMap().put("remove-selected-node", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeSelectedVertex();
            }
        });
    }

    private void applyTheme() {
        Color background = darkTheme ? new Color(24, 28, 35) : new Color(248, 250, 253);
        Color panel = darkTheme ? new Color(34, 40, 49) : Color.WHITE;
        Color foreground = darkTheme ? new Color(235, 239, 246) : new Color(35, 43, 54);
        Color input = darkTheme ? new Color(43, 51, 63) : Color.WHITE;
        Color outline = darkTheme ? new Color(82, 96, 115) : new Color(206, 215, 226);
        Color buttonColor = darkTheme ? new Color(54, 65, 80) : new Color(239, 243, 248);
        styleTree(getContentPane(), background, panel, foreground, input, outline, buttonColor);
        canvas.setDarkTheme(darkTheme);
        getContentPane().repaint();
    }

    private static void styleTree(Component component, Color background, Color panel,
                                  Color foreground, Color input, Color outline, Color buttonColor) {
        if (component instanceof JPanel panelComponent) {
            panelComponent.setBackground(panel);
            styleBorder(panelComponent.getBorder(), foreground, outline);
        } else if (component instanceof JScrollPane scroll) {
            scroll.setBackground(panel);
            scroll.getViewport().setBackground(panel);
            scroll.setBorder(BorderFactory.createLineBorder(outline));
        } else if (component instanceof JTabbedPane tabs) {
            tabs.setBackground(panel);
            tabs.setForeground(foreground);
            tabs.setOpaque(true);
            for (int index = 0; index < tabs.getTabCount(); index++) {
                tabs.setBackgroundAt(index, panel);
                tabs.setForegroundAt(index, foreground);
            }
        } else if (component instanceof JLabel label) {
            label.setForeground(foreground);
        } else if (component instanceof JTextArea area) {
            area.setBackground(input);
            area.setForeground(foreground);
            area.setCaretColor(foreground);
            area.setOpaque(true);
        } else if (component instanceof JTextField field) {
            field.setBackground(input);
            field.setForeground(foreground);
            field.setCaretColor(foreground);
            field.setOpaque(true);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(outline),
                    BorderFactory.createEmptyBorder(4, 7, 4, 7)));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setBackground(input);
            combo.setForeground(foreground);
            combo.setOpaque(true);
        } else if (component instanceof JButton button) {
            button.setBackground(buttonColor);
            button.setForeground(foreground);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(outline),
                    BorderFactory.createEmptyBorder(4, 9, 4, 9)));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                styleTree(child, background, panel, foreground, input, outline, buttonColor);
            }
        }
    }

    private static void styleBorder(javax.swing.border.Border border, Color foreground, Color outline) {
        if (border instanceof javax.swing.border.TitledBorder titled) {
            titled.setTitleColor(foreground);
            titled.setBorder(BorderFactory.createLineBorder(outline));
        } else if (border instanceof javax.swing.border.CompoundBorder compound) {
            styleBorder(compound.getOutsideBorder(), foreground, outline);
            styleBorder(compound.getInsideBorder(), foreground, outline);
        }
    }

    private void showError(String title, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private record AnalysisOutcome(String report, List<String> highlightedPath) {
    }

    private final class GraphCanvas extends JPanel {
        private Graph graph = new Graph();
        private final Map<String, Point2D.Double> points = new LinkedHashMap<>();
        private String selected;
        private String pulseNode;
        private List<String> highlightedPath = List.of();
        private Timer animation;

        private GraphCanvas() {
            setPreferredSize(new Dimension(1200, 780));
            setOpaque(true);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent event) {
                    String hit = findNode(event.getX(), event.getY());
                    selected = hit;
                    if (hit != null && SwingUtilities.isLeftMouseButton(event)) {
                        requestFocusInWindow();
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent event) {
                    if (selected != null) {
                        statusLabel.setText("Nodo seleccionado: " + selected
                                + " · arrastra para cambiar su posición.");
                    }
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2
                            && findNode(event.getX(), event.getY()) == null) {
                        addVertexAt(new Point2D.Double(event.getX(), event.getY()));
                    }
                }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(java.awt.event.MouseEvent event) {
                    if (selected != null) {
                        points.put(selected, new Point2D.Double(event.getX(), event.getY()));
                        repaint();
                    }
                }
            });
        }

        private void setGraph(Graph graph, Map<String, Point2D.Double> savedPositions) {
            this.graph = graph;
            points.clear();
            points.putAll(savedPositions);
            selected = null;
            highlightedPath = List.of();
            pulseNode = null;
            syncVertices();
        }

        private void syncVertices() {
            points.keySet().retainAll(graph.vertices());
            List<String> vertices = graph.sortedVertices();
            int count = Math.max(1, vertices.size());
            int index = 0;
            for (String vertex : vertices) {
                if (!points.containsKey(vertex)) {
                    double angle = -Math.PI / 2 + 2 * Math.PI * index / count;
                    points.put(vertex, new Point2D.Double(
                            600 + 260 * Math.cos(angle), 385 + 250 * Math.sin(angle)));
                }
                index++;
            }
            if (selected != null && !graph.vertices().contains(selected)) {
                selected = null;
            }
            repaint();
        }

        private void autoLayout() {
            points.clear();
            syncVertices();
        }

        private void setPosition(String vertex, Point2D.Double point) {
            if (graph.vertices().contains(vertex)) {
                points.put(vertex, point);
                repaint();
            }
        }

        private Map<String, Point2D.Double> positions() {
            return Collections.unmodifiableMap(points);
        }

        private String selectedVertex() {
            return selected;
        }

        private void clearSelection() {
            selected = null;
            repaint();
        }

        private void setDarkTheme(boolean dark) {
            setBackground(dark ? new Color(24, 28, 35) : new Color(248, 250, 253));
            repaint();
        }

        private void animatePath(List<String> path) {
            if (animation != null) {
                animation.stop();
            }
            highlightedPath = List.copyOf(path);
            if (path.isEmpty()) {
                pulseNode = null;
                repaint();
                return;
            }
            final int[] index = {0};
            pulseNode = path.get(0);
            animation = new Timer(420, event -> {
                if (index[0] >= path.size()) {
                    ((Timer) event.getSource()).stop();
                    pulseNode = null;
                } else {
                    pulseNode = path.get(index[0]++);
                }
                repaint();
            });
            animation.start();
        }

        private String findNode(double x, double y) {
            List<String> vertices = graph.sortedVertices();
            for (int index = vertices.size() - 1; index >= 0; index--) {
                String vertex = vertices.get(index);
                Point2D.Double center = points.get(vertex);
                if (center != null && Math.abs(x - center.x) <= nodeWidth(vertex) / 2.0 + 5
                        && Math.abs(y - center.y) <= 29) {
                    return vertex;
                }
            }
            return null;
        }

        private int nodeWidth(String vertex) {
            FontMetrics metrics = getFontMetrics(getFont().deriveFont(Font.BOLD, 12f));
            return Math.max(92, metrics.stringWidth(vertex) + 28);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawGrid(g);
            if (graph.vertices().isEmpty()) {
                g.setColor(darkTheme ? new Color(178, 188, 203) : new Color(113, 124, 139));
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 17f));
                g.drawString("Carga un archivo CSV o JSON para visualizar la red", 80, 110);
                g.dispose();
                return;
            }
            drawEdges(g);
            drawNodes(g);
            g.dispose();
        }

        private void drawGrid(Graphics2D g) {
            Color dot = darkTheme ? new Color(54, 62, 74) : new Color(225, 231, 239);
            g.setColor(dot);
            for (int x = 24; x < getWidth(); x += 28) {
                for (int y = 24; y < getHeight(); y += 28) {
                    g.fillOval(x, y, 2, 2);
                }
            }
        }

        private void drawEdges(Graphics2D g) {
            Color normal = darkTheme ? new Color(134, 150, 172) : new Color(119, 134, 153);
            Color active = new Color(17, 167, 148);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (String from : graph.sortedVertices()) {
                for (String to : graph.directedNeighbors(from)) {
                    Point2D.Double source = points.get(from);
                    Point2D.Double target = points.get(to);
                    if (source == null || target == null) {
                        continue;
                    }
                    boolean highlighted = pathContainsEdge(from, to);
                    g.setColor(highlighted ? active : normal);
                    g.setStroke(new BasicStroke(highlighted ? 3.0f : 1.7f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    if (from.equals(to)) {
                        drawSelfLoop(g, source, nodeWidth(from) / 2.0);
                    } else {
                        drawArrow(g, from, to, source, target);
                    }
                }
            }
        }

        private void drawArrow(Graphics2D g, String from, String to,
                               Point2D.Double source, Point2D.Double target) {
            double dx = target.x - source.x;
            double dy = target.y - source.y;
            double distance = Math.max(1, Math.hypot(dx, dy));
            double ux = dx / distance;
            double uy = dy / distance;
            double perpendicularX = -uy;
            double perpendicularY = ux;
            boolean reverse = graph.directedNeighbors(to).contains(from);
            double offset = reverse ? (from.compareTo(to) < 0 ? 11 : -11) : 0;
            double startRadius = Math.max(nodeWidth(from) / 2.0, 46);
            double endRadius = Math.max(nodeWidth(to) / 2.0, 46);
            double sx = source.x + ux * startRadius;
            double sy = source.y + uy * 25;
            double ex = target.x - ux * endRadius;
            double ey = target.y - uy * 25;
            double cx = (sx + ex) / 2 + perpendicularX * offset;
            double cy = (sy + ey) / 2 + perpendicularY * offset;
            QuadCurve2D curve = new QuadCurve2D.Double(sx, sy, cx, cy, ex, ey);
            g.draw(curve);
            double tx = ex - cx;
            double ty = ey - cy;
            double angle = Math.atan2(ty, tx);
            int size = 9;
            double a1 = angle + Math.PI * 0.83;
            double a2 = angle - Math.PI * 0.83;
            g.drawLine((int) ex, (int) ey, (int) (ex + size * Math.cos(a1)), (int) (ey + size * Math.sin(a1)));
            g.drawLine((int) ex, (int) ey, (int) (ex + size * Math.cos(a2)), (int) (ey + size * Math.sin(a2)));
        }

        private void drawSelfLoop(Graphics2D g, Point2D.Double center, double radius) {
            Rectangle2D loop = new Rectangle2D.Double(center.x - 16, center.y - 49,
                    32, 27);
            g.drawOval((int) loop.getX(), (int) loop.getY(), (int) loop.getWidth(), (int) loop.getHeight());
            g.drawLine((int) (center.x + 13), (int) (center.y - 36),
                    (int) (center.x + 9), (int) (center.y - 43));
            g.drawLine((int) (center.x + 13), (int) (center.y - 36),
                    (int) (center.x + 5), (int) (center.y - 38));
        }

        private boolean pathContainsEdge(String from, String to) {
            for (int index = 0; index + 1 < highlightedPath.size(); index++) {
                String first = highlightedPath.get(index);
                String second = highlightedPath.get(index + 1);
                if ((first.equals(from) && second.equals(to))
                        || (first.equals(to) && second.equals(from))) {
                    return true;
                }
            }
            return false;
        }

        private void drawNodes(Graphics2D g) {
            Font font = getFont().deriveFont(Font.BOLD, 12f);
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics();
            for (String vertex : graph.sortedVertices()) {
                Point2D.Double center = points.get(vertex);
                if (center == null) {
                    continue;
                }
                int width = nodeWidth(vertex);
                int height = 48;
                int x = (int) Math.round(center.x - width / 2.0);
                int y = (int) Math.round(center.y - height / 2.0);
                boolean isSelected = vertex.equals(selected);
                boolean isPulse = vertex.equals(pulseNode);
                Color fill = isSelected ? new Color(18, 155, 140)
                        : (darkTheme ? new Color(52, 61, 74) : Color.WHITE);
                Color outline = isSelected ? new Color(12, 130, 117)
                        : (darkTheme ? new Color(110, 129, 154) : new Color(187, 198, 211));
                if (isPulse) {
                    g.setColor(new Color(18, 155, 140, 60));
                    g.fillRoundRect(x - 8, y - 8, width + 16, height + 16, 30, 30);
                }
                g.setColor(fill);
                g.fillRoundRect(x, y, width, height, 24, 24);
                g.setColor(outline);
                g.setStroke(new BasicStroke(isSelected ? 2.4f : 1.4f));
                g.drawRoundRect(x, y, width, height, 24, 24);
                g.setColor(isSelected ? Color.WHITE
                        : (darkTheme ? new Color(230, 235, 242) : new Color(37, 48, 62)));
                int textX = (int) Math.round(center.x - metrics.stringWidth(vertex) / 2.0);
                int textY = (int) Math.round(center.y + (metrics.getAscent() - metrics.getDescent()) / 2.0);
                g.drawString(vertex, textX, textY);
            }
        }
    }
}

