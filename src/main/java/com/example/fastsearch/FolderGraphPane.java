package com.example.fastsearch;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.input.MouseButton;
import java.util.*;
import java.util.function.Consumer;

public final class FolderGraphPane extends Pane {
    private static final double W = 170, H = 50, HGAP = 18, VGAP = 60, ROOT_TOP = 50;
    private static final int MAX_CHILDREN = 40;
    private static final int MAX_NODES = 600;     // max nodes

    // color scale logarithmic
    private static final double LOG_LO = Math.log10(0.0001), LOG_HI = Math.log10(0.05);

    private final Group edges = new Group(), nodes = new Group();
    private final Group world = new Group(edges, nodes);
    private final Translate tr = new Translate();
    private final Scale sc = new Scale(1, 1, 0, 0);

    private final Set<ResultDir> userCollapsed = new HashSet<>();
    private final Set<ResultDir> userExpanded = new HashSet<>();
    private final Set<ResultDir> autoOpen = new HashSet<>();
    private final Map<ResultDir, List<ResultDir>> shown = new HashMap<>();
    private final Map<ResultDir, Double> widths = new HashMap<>();

    private List<ResultDir> roots = List.of();
    private Consumer<ResultDir> onSelect = d -> {};
    private double lastX, lastY;
    private boolean fitted;

    public FolderGraphPane() {
        world.getTransforms().addAll(tr, sc);
        getChildren().add(world);
        setStyle("-fx-background-color:#fafafa;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        setOnMousePressed(e -> { lastX = e.getX(); lastY = e.getY(); });
        setOnMouseDragged(e -> {
            tr.setX(tr.getX() + e.getX() - lastX);
            tr.setY(tr.getY() + e.getY() - lastY);
            lastX = e.getX(); lastY = e.getY();
        });
        setOnScroll(e -> {
            double f = e.getDeltaY() > 0 ? 1.12 : 1 / 1.12;
            double s = Math.max(0.03, Math.min(3, sc.getX() * f));
            double lx = (e.getX() - tr.getX()) / sc.getX(), ly = (e.getY() - tr.getY()) / sc.getY();
            sc.setX(s); sc.setY(s);
            tr.setX(e.getX() - lx * s); tr.setY(e.getY() - ly * s);
            e.consume();
        });
    }

    public void setOnFolderSelected(Consumer<ResultDir> c) { onSelect = c; }

    public void setRoots(List<ResultDir> r) {
        roots = r;
        fitted = false;
        refresh();
    }

    // ------------------------------------------------------------------ nyitás-logika

    private boolean isOpen(ResultDir d) {
        return !userCollapsed.contains(d) && (userExpanded.contains(d) || autoOpen.contains(d));
    }


    private void computeAutoOpen() {
        autoOpen.clear();
        int count = roots.size();
        PriorityQueue<ResultDir> pq = new PriorityQueue<>(Comparator.comparingLong(ResultDir::getTotalBytes).reversed());
        for (ResultDir r : roots) if (r.hasChildren()) pq.add(r);
        while (!pq.isEmpty() && count < MAX_NODES) {
            ResultDir d = pq.poll();
            if (userCollapsed.contains(d)) continue;
            autoOpen.add(d);
            for (ResultDir c : shown(d)) {
                count++;
                if (c.hasChildren()) pq.add(c);
            }
        }
    }

    // ------------------------------------------------------------------ szín

    private static long capacity(ResultDir d) {
        for (ResultDir x = d; x != null; x = x.getParent()) {
            if (x.getVolume() != null && (x.getParent() == null || x.isMount())) {
                long t = x.getVolume().total();
                if (t > 0) return t;
                break;
            }
        }
        ResultDir top = d;
        while (top.getParent() != null) top = top.getParent();
        return Math.max(1, top.getTotalBytes());
    }

    private static double weight(ResultDir d) {
        double ratio = Math.max((double) d.getTotalBytes() / capacity(d), 1e-9);
        double t = (Math.log10(ratio) - LOG_LO) / (LOG_HI - LOG_LO);
        return Math.max(0, Math.min(1, t));
    }

    private static Color fill(double t)   { return Color.hsb(50 * (1 - t), 0.35 + 0.50 * t, 1.0); }
    private static Color border(double t) { return Color.hsb(50 * (1 - t), 0.80, 0.65); }
    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }

    // ------------------------------------------------------------------ layout

    private List<ResultDir> shown(ResultDir d) {
        return shown.computeIfAbsent(d, k -> {
            List<ResultDir> c = k.childrenSnapshot();
            c.sort(Comparator.comparingLong(ResultDir::getTotalBytes).reversed());
            return c.size() > MAX_CHILDREN ? new ArrayList<>(c.subList(0, MAX_CHILDREN)) : c;
        });
    }

    private double width(ResultDir d) {
        Double cached = widths.get(d);
        if (cached != null) return cached;
        double w = W;
        if (isOpen(d)) {
            List<ResultDir> ch = shown(d);
            if (!ch.isEmpty()) {
                double span = -HGAP;
                for (ResultDir c : ch) span += width(c) + HGAP;
                w = Math.max(W, span);
            }
        }
        widths.put(d, w);
        return w;
    }

    public void refresh() {
        shown.clear(); widths.clear();
        computeAutoOpen();
        edges.getChildren().clear(); nodes.getChildren().clear();

        double x = 0;
        for (ResultDir r : roots) {
            double w = width(r);
            Label vol = new Label(r.getVolume() != null ? r.getVolume().describe(r.getPath().toString())
                    : r.getPath().toString());
            vol.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#444;");
            vol.setLayoutX(x); vol.setLayoutY(14);
            nodes.getChildren().add(vol);
            place(r, x, ROOT_TOP);
            x += w + 140;
        }
        if (!fitted && getWidth() > 0 && !roots.isEmpty()) { fit(); fitted = true; }
    }

    private void place(ResultDir d, double left, double y) {
        double w = width(d), cx = left + w / 2;
        addNode(d, cx - W / 2, y);
        if (!isOpen(d)) return;
        List<ResultDir> ch = shown(d);
        if (ch.isEmpty()) return;

        double span = -HGAP;
        for (ResultDir c : ch) span += width(c) + HGAP;
        double start = left + (w - span) / 2;
        double cy = y + H + VGAP, midY = y + H + VGAP / 2;
        for (ResultDir c : ch) {
            double cw = width(c), ccx = start + cw / 2;
            edge(cx, y + H, ccx, cy, midY, weight(c));
            place(c, start, cy);
            start += cw + HGAP;
        }
    }

    private void edge(double px, double py, double cx, double cy, double midY, double t) {
        Color c = border(t);
        Polyline pl = new Polyline(px, py, px, midY, cx, midY, cx, cy - 9);
        pl.setFill(null);
        pl.setStroke(c);
        pl.setStrokeWidth(1.5 + 2.0 * t);
        Polygon head = new Polygon(cx, cy, cx - 5, cy - 9, cx + 5, cy - 9);
        head.setFill(c);
        pl.setMouseTransparent(true); head.setMouseTransparent(true);
        edges.getChildren().addAll(pl, head);
    }

    private static void openInExplorer(ResultDir d) {
        try {
            new ProcessBuilder("explorer.exe", d.getPath().toString()).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addNode(ResultDir d, double x, double y) {
        boolean kids = d.hasChildren(), open = isOpen(d);
        int hidden = open ? d.childrenSnapshot().size() - shown(d).size() : 0;
        double t = weight(d);

        VBox box = new VBox(2);
        box.setLayoutX(x); box.setLayoutY(y);
        box.setPrefSize(W, H); box.setMinSize(W, H); box.setMaxSize(W, H);
        box.setAlignment(Pos.CENTER);
        String bc = d.isMount() ? "#1e88e5" : hex(border(t));
        box.setStyle("-fx-background-color:" + hex(fill(t)) + ";-fx-border-color:" + bc + ";-fx-border-width:"
                + (d.isMount() ? 2.5 : 1.5) + ";-fx-background-radius:6;-fx-border-radius:6;-fx-padding:4 6;");
        box.setCursor(Cursor.HAND);

        Label n = new Label((kids ? (open ? "▾ " : "▸ ") : "") + (d.isMount() ? "⛁ " : "") + d.getName());
        n.setMaxWidth(W - 14);
        n.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:#222;");
        Label s = new Label(Util.size(d.getTotalBytes()) + (hidden > 0 ? "  (+" + hidden + " more)" : ""));
        s.setStyle("-fx-font-size:11px;-fx-text-fill:#333;");
        box.getChildren().addAll(n, s);

        box.setOnMouseClicked(e -> {
            if (!e.isStillSincePress()) return;
            if (!e.isPrimaryButtonDown()) return;
            if (e.getButton() == MouseButton.SECONDARY) {
                openInExplorer(d); // open in explorer
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) return;   // open / clouse
            if (open) { userCollapsed.add(d); userExpanded.remove(d); }
            else      { userExpanded.add(d);  userCollapsed.remove(d); }
            onSelect.accept(d);
            refresh();
        });
        nodes.getChildren().add(box);
    }

    public void fit() {
        Bounds b = world.getBoundsInLocal();
        if (b.getWidth() <= 0 || getWidth() <= 0) return;
        double s = Math.min(1, Math.min(getWidth() / (b.getWidth() + 40), getHeight() / (b.getHeight() + 40)));
        s = Math.max(s, 0.03);
        sc.setX(s); sc.setY(s);
        tr.setX((getWidth() - b.getWidth() * s) / 2 - b.getMinX() * s);
        tr.setY(20 - b.getMinY() * s);
    }
}