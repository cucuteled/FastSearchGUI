package com.example.fastsearch;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.util.*;
import java.util.function.Consumer;

public final class ColumnGrid extends Region {
    private static final double ROW_H = 64, BAR_W = 14;

    private final int columns;
    private final LiveResults data;
    private final Consumer<Result> onOpen, onDelete;
    private final ScrollBar bar = new ScrollBar();
    private final List<Card> cards = new ArrayList<>();
    private int rows = 0, firstCol = 0;
    private boolean syncing;

    public ColumnGrid(LiveResults data, int columns, Consumer<Result> onOpen, Consumer<Result> onDelete) {
        this.data = data; this.columns = columns; this.onOpen = onOpen; this.onDelete = onDelete;

        bar.setOrientation(javafx.geometry.Orientation.VERTICAL);
        bar.setUnitIncrement(1);
        bar.setBlockIncrement(columns);
        getChildren().add(bar);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        bar.valueProperty().addListener((o, a, b) -> {
            if (syncing) return;
            firstCol = (int) Math.round(b.doubleValue());
            refresh();
        });
        setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            firstCol += e.getDeltaY() < 0 ? 1 : -1;
            refresh();
            e.consume();
        });
    }

    @Override protected void layoutChildren() {
        double w = getWidth(), h = getHeight();
        bar.resizeRelocate(w - BAR_W, 0, BAR_W, h);

        int newRows = Math.max(1, (int) Math.ceil(h / ROW_H));   // ceil a little more
        boolean rebuilt = false;
        if (newRows != rows || cards.isEmpty()) {
            rows = newRows;
            getChildren().removeAll(cards);
            cards.clear();
            for (int i = 0; i < rows * columns; i++) { Card c = new Card(); cards.add(c); }
            getChildren().addAll(cards);
            rebuilt = true;
        }
        double cw = (w - BAR_W) / columns;
        for (int c = 0; c < columns; c++)
            for (int r = 0; r < rows; r++)
                cards.get(c * rows + r).resizeRelocate(c * cw + 2, r * ROW_H + 2, cw - 4, ROW_H - 4);
        if (rebuilt) refresh();
    }

    public void refresh() {
        if (rows == 0) return;
        int total = data.size();
        int totalCols = Math.max(1, (int) Math.ceil(total / (double) rows));
        int maxFirst = Math.max(0, totalCols - columns);
        firstCol = Math.max(0, Math.min(firstCol, maxFirst));

        syncing = true;
        bar.setMin(0);
        bar.setMax(totalCols);
        bar.setVisibleAmount(Math.min(columns, totalCols));
        bar.setValue(firstCol);
        syncing = false;

        int base = firstCol * rows;
        for (int i = 0; i < cards.size(); i++) {
            int idx = base + i;                       // i = c*rows + r
            cards.get(i).bind(idx < total ? data.get(idx) : null);
        }
    }

    // ------------------------------------------------------------------ kártya

    private static final String OPEN_SVG = "M9 2h5v5 M14 2L7 9 M12 10v4H2V4h4";
    private static final String DEL_SVG  = "M2 4h12 M6 4V2h4v2 M3.5 4l.8 10h7.4l.8-10 M6.5 7v5 M9.5 7v5";

    private final class Card extends HBox {
        private final ImageView icon = new ImageView();
        private final Label name = new Label(), meta = new Label();
        private final Button open = iconButton(OPEN_SVG, "Open in Explorer", "#1565c0");
        private final Button del  = iconButton(DEL_SVG, "Delete", "#c62828");
        private final Tooltip tip = new Tooltip();
        private Result r;

        Card() {
            setSpacing(8);
            setAlignment(Pos.CENTER_LEFT);
            setStyle("-fx-background-color:#f4f4f4;-fx-padding:6 8;-fx-border-color:#cccccc;"
                    + "-fx-background-radius:4;-fx-border-radius:4;");
            icon.setFitWidth(32); icon.setFitHeight(32);

            name.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
            name.setMinWidth(0);                       // a név zsugorodhat (…), a gombok NEM
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            meta.setStyle("-fx-font-style:italic;-fx-text-fill:#777777;-fx-font-size:11px;");

            HBox top = new HBox(4, name, open, del);   // gombok egymás mellett, jobb felül
            top.setAlignment(Pos.TOP_RIGHT);
            VBox info = new VBox(2, top, meta);
            info.setMinWidth(0);
            HBox.setHgrow(info, Priority.ALWAYS);
            getChildren().addAll(icon, info);

            open.setOnAction(e -> { if (r != null) onOpen.accept(r); });
            del.setOnAction(e -> { if (r != null) onDelete.accept(r); });
            Tooltip.install(this, tip);
        }

        void bind(Result r) {
            this.r = r;
            setVisible(r != null);
            if (r == null) return;
            name.setText(r.getName());
            meta.setText(Util.size(r.getSize()) + "  ·  " + r.getDate());
            icon.setImage(iconFor(Util.ext(r.getName())));
            tip.setText(r.getPath());
        }
    }

    private static Button iconButton(String svg, String tip, String color) {
        SVGPath p = new SVGPath();
        p.setContent(svg);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(color));
        p.setStrokeWidth(1.6);
        Button b = new Button();
        b.setGraphic(p);
        b.setTooltip(new Tooltip(tip));
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setFocusTraversable(false);
        b.setStyle("-fx-background-color:white;-fx-border-color:" + color
                + ";-fx-border-radius:4;-fx-background-radius:4;-fx-padding:3 5;-fx-cursor:hand;");
        return b;
    }

    // ------------------------------------------------------------------ ikon cache

    private static final String BASE = "/com/example/fastsearch/icons/";
    private static final Map<String, Image> ICONS = new HashMap<>();
    private static final Map<String, String> GROUP = new HashMap<>();
    static {
        grp("music",  "mp3,wav,flac,aac,ogg,wma,m4a,alac");
        grp("video",  "mp4,mov,mkv,avi,wmv,flv,mpeg,mpg,webm,3gp");
        grp("docs",   "doc,docx,pdf,ppt,pptx,xls,xlsx,odt,rtf");
        grp("images", "jpg,jpeg,png,gif,bmp,tiff,svg,webp,heic");
    }
    private static void grp(String icon, String exts) { for (String e : exts.split(",")) GROUP.put(e, icon); }

    private static Image iconFor(String ext) {
        String key = GROUP.getOrDefault(ext, ext.isEmpty() ? "default" : ext);
        Image img = ICONS.get(key);
        if (img == null) {
            var in = ColumnGrid.class.getResourceAsStream(BASE + key + ".png");
            if (in == null) in = ColumnGrid.class.getResourceAsStream(BASE + "default.png");
            if (in == null) return null;
            img = new Image(in, 32, 64, true, true);
            ICONS.put(key, img);
        }
        return img;
    }
}