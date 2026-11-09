package com.example.fastsearch;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.net.URL;

import java.time.LocalDate;

import java.util.*;

public class SearchController implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (app.STATE.isSpecifiedLocation) {
            Thread thread = new Thread(() -> {
                app.STATE.activeSearchThreads.incrementAndGet();
                try {
                    ResultDir rd = new ResultDir(new File(app.STATE.selectedPath), null);
                    new Search(rd);
                } finally {
                    app.STATE.activeSearchThreads.decrementAndGet();
                }
            });
            thread.start();
        } else {
            for (String path : app.STATE.selectedDrives) {
                Thread thread = new Thread(() -> {
                    app.STATE.activeSearchThreads.incrementAndGet();
                    try {
                        ResultDir rd = new ResultDir(new File(path), null);
                        new Search(rd);
                    } finally {
                        app.STATE.activeSearchThreads.decrementAndGet();
                    }
                });
                thread.start();
            }
        }
        countThreads();
        System.out.println("Search initiated");
    }

    //List Handle
    private void countThreads() {
        Thread thread = new Thread(() -> {
            int remaining = app.STATE.activeSearchThreads.get();
            long timeoutMillis = 6000; // 6 másodperc
            long lastChangeTime = System.currentTimeMillis();
            int lastProcessed = app.STATE.ProcessedFiles;

            while (true) {
                remaining = app.STATE.activeSearchThreads.get();

                if (remaining <= 0) break;

                // Ha nem változott a processed szám
                if (app.STATE.ProcessedFiles == lastProcessed) {
                    //System.out.println("rem:" + remaining);
                    if (System.currentTimeMillis() - lastChangeTime > timeoutMillis) {
                        break; // 6 másodpercig nem változott, kilépünk
                    }
                } else {
                    lastProcessed = app.STATE.ProcessedFiles;
                    lastChangeTime = System.currentTimeMillis();
                }

                // GUI frissítés
                Platform.runLater(() -> ProgressLabel.setText("Processed: " + app.STATE.ProcessedFiles));

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            // ha vége:
            System.out.println("kész!");
            Platform.runLater(() -> {
                resultsBox.setPrefHeight(435);
                scrollPane.setPrefHeight(435);
                ProgressLabel.setVisible(false);
                addResultToList();
            });
        });
        thread.start();
    }


    @FXML
    private VBox resultsBox;

    @FXML
    private ScrollPane scrollPane;

    List<Result> talalatok = new ArrayList<>();

    public void addResultToList() {

        // clear list:
        resultsBox.getChildren().clear();
        talalatok.clear();

        if (app.STATE.results.isEmpty()) {
            // adjunk hozzá: nincs találat:
            Label noresult = new Label("No result");
            resultsBox.getChildren().add(noresult);
            return;
        }


        // nameFilter:
        if (!nameFilterField.getText().isEmpty()) {
            for (Result rs : app.STATE.results) {
                if (rs.getPath().toLowerCase().contains(nameFilterField.getText().toLowerCase())) talalatok.add(rs);
            }
        } else {
            talalatok = new ArrayList<>(app.STATE.results);
        }
        if (talalatok.isEmpty()) return;

        // sort:
        Collections.sort(talalatok);
        if (!app.STATE.Descending) Collections.reverse(talalatok);

        for (Result r : talalatok) {

            // Egy sor
            HBox row = new HBox();
            row.setSpacing(10);
            row.setPrefHeight(80);
            row.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10; -fx-border-color: #cccccc; -fx-background-radius: 4;");
            row.setAlignment(Pos.CENTER_LEFT);

            // Bal oldali ikon
            ImageView icon = new ImageView();
            icon.setFitWidth(48);
            icon.setFitHeight(48);
            icon.setImage(getIconForFile(r.getjFile()));

            // Jobb oldali szövegek
            VBox infoBox = new VBox();
            infoBox.setSpacing(5);

            Label nameLabel = new Label(r.getFileName());
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            double mb = r.getBytes() / 1024.0 / 1024.0;
            Label sizeLabel = new Label(String.format("Size: %.2f MB", mb));
            sizeLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #777777;");

            LocalDate date = r.getLastModified();
            Label dateLabel = new Label("Modified: " + date);
            dateLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #777777;");

            infoBox.getChildren().addAll(nameLabel, sizeLabel, dateLabel);

            // Jobb oldali gombok
            VBox buttonBox = new VBox();
            buttonBox.setSpacing(5);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            Button openBtn = new Button("Open");
            openBtn.setOnAction(e -> {
                try {
                    String path = r.getPath();
                    // Windows Explorer megnyitás és kijelölés
                    new ProcessBuilder("explorer.exe", "/select,", path).start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Confirmation");
                alert.setHeaderText(null);
                alert.setContentText("Are you sure you want to delete this file?");

                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        if (r.getjFile().delete()) {
                            resultsBox.getChildren().remove(row); // törlés után eltávolítjuk a sorból
                        } else {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setHeaderText(null);
                            err.setContentText("Failed to delete the file.");
                            err.showAndWait();
                        }
                    }
                });
            });

            buttonBox.getChildren().addAll(openBtn, deleteBtn);

            // Sor összeállítása
            Region spacer = new Region(); // a spacer tolja jobbra a gombokat
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(icon, infoBox, spacer, buttonBox);

            // VBox-ba felvétel
            resultsBox.getChildren().add(row);
        }
    }



    private Image getIconForFile(File file) {

        String base = "/com/example/fastsearch/icons/";

        if (file.isDirectory()) {
            return new Image(getClass().getResourceAsStream(base + "folder.png"));
        }

        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf(".");
        String ext = (dot == -1) ? "" : name.substring(dot + 1);

        String music = "mp3,wav,flac,aac,ogg,wma,m4a,alac,";
        String video = "mp4,mov,mkv,avi,wmv,flv,mpeg,mpg,webm,3gp,";
        String docs  = "doc,docx,pdf,ppt,pptx,xls,xlsx,odt,rtf,";
        String images = "jpg,jpeg,png,gif,bmp,tiff,svg,webp,heic,";

        if (music.contains(ext)) return new Image(getClass().getResourceAsStream(base + "music.png"));
        if (docs.contains(ext)) return new Image(getClass().getResourceAsStream(base + "docs.png"));
        if (video.contains(ext)) return new Image(getClass().getResourceAsStream(base + "video.png"));
        if (images.contains(ext)) return new Image(getClass().getResourceAsStream(base + "images.png"));

        try {
            return new Image(getClass().getResourceAsStream(base + ext + ".png"));
        } catch (Exception e) {
            return new Image(getClass().getResourceAsStream(base + "default.png"));
        }
    }

    @FXML
    public void initSearch() {
        addResultToList();
    }

    private final FSApplication app = FSApplication.getInstance();

    @FXML
    private ToggleButton arrowButton;

    @FXML
    private TextField nameFilterField;

    @FXML
    private Label ProgressLabel;

    // Actions:

    @FXML
    private void setOrderingMode() {
        if (arrowButton.getText().equals("🔽")) {
            app.STATE.Descending = false;
            arrowButton.setText("🔼");
        } else {
            app.STATE.Descending = true;
            arrowButton.setText("🔽");
        }
        addResultToList();
    }

    @FXML
    private void orderByName() {
        app.STATE.orderMode = "orderByName";
        addResultToList();
    }

    @FXML
    private void orderByTime() {
        app.STATE.orderMode = "orderByTime";
        addResultToList();
    }

    @FXML
    private void orderByType() {
        app.STATE.orderMode = "orderByType";
        addResultToList();
    }

    @FXML
    private void orderBySize() {
        app.STATE.orderMode = "orderBySize";
        addResultToList();
    }

}
