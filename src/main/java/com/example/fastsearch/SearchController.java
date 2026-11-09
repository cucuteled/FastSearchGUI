package com.example.fastsearch;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SearchController implements Initializable {

    private static final String[] GEAR = {"/.\\", "-.-", "\\./", "|.|"};

    @FXML private StackPane content;
    @FXML private Label gearLabel, statusLabel;
    @FXML private TextField nameFilterField, extFilterField, minMbField, maxMbField;
    @FXML private ToggleButton arrowButton, listToggle, treeToggle;
    @FXML private ToggleGroup viewGroup;

    private final FSApplication app = FSApplication.getInstance();
    private final IndexStore index = app.STATE.index;

    private SearchFilter base;
    private LiveResults live;
    private ColumnGrid grid;
    private FolderGraphPane graph;
    private final PauseTransition debounce = new PauseTransition(Duration.millis(150));

    private long lastProcessed = -1, lastProgressAt = System.currentTimeMillis(), lastTreeRefresh;
    private int frame;
    private boolean doneShown;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        base = SearchFilter.fromState(app.STATE);
        nameFilterField.setText(base.name());
        extFilterField.setText((base.ignoreExts() ? "!" : "") + String.join(",", base.exts()));
        minMbField.setText(base.minBytes() > 0 ? String.valueOf(base.minBytes() >> 20) : "");
        maxMbField.setText(base.maxBytes() > 0 ? String.valueOf(base.maxBytes() >> 20) : "");

        live = new LiveResults(index, base, Result.comparator(app.STATE.orderMode), app.STATE.Descending);
        grid = new ColumnGrid(live, 3, this::openInExplorer, this::deleteFile);
        graph = new FolderGraphPane();
        graph.setVisible(false);
        content.getChildren().addAll(grid, graph);

        viewGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == null) { old.setSelected(true); return; }
            boolean tree = sel == treeToggle;
            grid.setVisible(!tree);
            graph.setVisible(tree);
            if (tree) { index.roots.forEach(ResultDir::finalizeSizes); graph.refresh(); }
        });

        // szűrő menet közben: debounce, majd az EGÉSZ indexre újraértékeljük
        debounce.setOnFinished(e -> live.setFilter(readFilter()));
        for (TextField f : List.of(nameFilterField, extFilterField, minMbField, maxMbField))
            f.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        startCrawl();

        Timeline ticker = new Timeline(new KeyFrame(Duration.millis(120), e -> tick()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    private void startCrawl() {
        List<Path> paths = new ArrayList<>();
        if (app.STATE.isSpecifiedLocation) paths.add(Path.of(app.STATE.selectedPath));
        else for (String d : app.STATE.selectedDrives) paths.add(Path.of(d));

        new Crawler(index, app.executor, app.STATE.DISALLOWED_PATH_SEGMENTS).startAll(paths);
        graph.setRoots(index.roots);
    }

    // ------------------------------------------------------------------ élő frissítés

    private void tick() {
        long now = System.currentTimeMillis();
        long p = index.processed.get();
        boolean running = !index.finished;

        if (p != lastProcessed) { lastProcessed = p; lastProgressAt = now; frame++; }

        if (running) {
            long stuck = now - lastProgressAt;
            if (stuck > 5000) {   // lefagyás jelző: a kerék megáll, kiírjuk hol akadt
                gearLabel.setText("(!)");
                statusLabel.setText("⚠ No progress for " + stuck / 1000 + " s – stuck at: " + index.lastPath);
            } else {
                gearLabel.setText(GEAR[frame % GEAR.length]);
                statusLabel.setText(String.format("Processed: %,d   ·   matches: %,d", p, live.size()));
            }
        }

        if (live.pump(!running)) grid.refresh();

        if (running && graph.isVisible() && now - lastTreeRefresh > 2000) {
            index.roots.forEach(ResultDir::finalizeSizes);
            graph.refresh();
            lastTreeRefresh = now;
        }

        if (!running && !doneShown) {
            doneShown = true;
            gearLabel.setText("✔");
            long total = index.roots.stream().mapToLong(ResultDir::getTotalBytes).sum();
            statusLabel.setText(String.format("Done · %,d files indexed · %,d matches · %s", p, live.size(), Util.size(total)));
            graph.refresh();
        }
    }

    private SearchFilter readFilter() {
        String ext = extFilterField.getText().trim().toLowerCase(Locale.ROOT);
        boolean ignore = ext.startsWith("!");
        if (ignore) ext = ext.substring(1);
        Set<String> exts = new HashSet<>();
        for (String e : ext.split("[,; ]+")) { e = e.replaceFirst("^\\.", ""); if (!e.isEmpty()) exts.add(e); }
        return new SearchFilter(nameFilterField.getText().trim().toLowerCase(Locale.ROOT), exts, ignore,
                mb(minMbField.getText()), mb(maxMbField.getText()), base.modifiedFrom(), base.modifiedTo());
    }

    private static long mb(String s) {
        try { return s == null || s.isBlank() ? 0 : (long) (Double.parseDouble(s.trim().replace(',', '.')) * 1048576); }
        catch (NumberFormatException e) { return 0; }
    }

    // ------------------------------------------------------------------ műveletek

    private void openInExplorer(Result r) {
        try { new ProcessBuilder("explorer.exe", "/select,", r.getPath()).start(); }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    private void deleteFile(Result r) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + r.getName() + " ?");
        a.setHeaderText(null);
        a.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.OK) return;
            try {
                Files.delete(Path.of(r.getPath()));
                r.markDeleted();
                live.remove(r);
                grid.refresh();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to delete the file.");
                err.setHeaderText(null);
                err.showAndWait();
            }
        });
    }

    @FXML private void setOrderingMode() {
        boolean asc = arrowButton.getText().equals("🔼");
        app.STATE.Descending = asc;
        arrowButton.setText(asc ? "🔽" : "🔼");
        live.setOrder(Result.comparator(app.STATE.orderMode), asc);
    }
    @FXML private void orderByName() { setOrder("orderByName"); }
    @FXML private void orderByTime() { setOrder("orderByTime"); }
    @FXML private void orderByType() { setOrder("orderByType"); }
    @FXML private void orderBySize() { setOrder("orderBySize"); }

    private void setOrder(String mode) {
        app.STATE.orderMode = mode;
        live.setOrder(Result.comparator(mode), app.STATE.Descending);
    }
}