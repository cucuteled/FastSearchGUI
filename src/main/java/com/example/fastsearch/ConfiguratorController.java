package com.example.fastsearch;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ConfiguratorController {
    // Elements:

    FSApplication app = FSApplication.getInstance();

    @FXML
    private Pane DriveSelectPane;

    @FXML
    private Pane PathSelectPane;

    @FXML
    private TextField PathFieldText;

    @FXML
    private ListView<CheckBox> driveChoiceBox;

    @FXML Button welcomeNext;

    // Actions:

    @FXML
    public void selectYes() {
        app.STATE.isSpecifiedLocation = true;
        DriveSelectPane.setVisible(true);
        PathSelectPane.setVisible(false);
    }

    @FXML
    public void selectNo() {
        app.STATE.isSpecifiedLocation = false;
        fillDrives();
        DriveSelectPane.setVisible(false);
        PathSelectPane.setVisible(true);
    }

    private void fillDrives() {
        driveChoiceBox.getItems().clear();
        for (File root : File.listRoots()) {
            if (!root.canRead() || !root.exists()) {
                continue;
            }
            CheckBox cbox = new CheckBox(root.getAbsolutePath());
            cbox.setSelected(true);
            driveChoiceBox.getItems().add(cbox);
        }

        welcomeNext.setDisable(driveChoiceBox.getItems().isEmpty());
    }

    @FXML
    public void openFileBrowser() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");

        File selected = chooser.showDialog(null);
        if (selected != null) {
            PathFieldText.setText(selected.getAbsolutePath());
            app.STATE.selectedPath = selected.getAbsolutePath();
            welcomeNext.setDisable(false);
            return;
        }
        welcomeNext.setDisable(true);
    }

    @FXML
    public void MyUser() {
        String userHome = System.getProperty("user.home");
        PathFieldText.setText(userHome);
        welcomeNext.setDisable(false);
    }


    @FXML
    public void MyDocuments() {
        Path documents = Path.of(System.getProperty("user.home"), "Documents");
        PathFieldText.setText(documents.toString());
        welcomeNext.setDisable(false);
    }

    @FXML
    public void Next() throws IOException {

        if (!app.STATE.isSpecifiedLocation) {
            Boolean isSomethingSelected = false;
            for (CheckBox c : driveChoiceBox.getItems()) {
                if (c.isSelected()) {
                    app.STATE.selectedDrives.add(c.getText());
                    isSomethingSelected = true;
                }
            }
            if (!isSomethingSelected) {
                welcomeNext.setDisable(true);
                return;
            }
        } else {
            if (new File(PathFieldText.getText()).isDirectory()) {
                app.STATE.selectedPath = PathFieldText.getText();
            } else {
                welcomeNext.setDisable(true);
                return;
            }
        }

        app.loadScene("options-view.fxml", 400, 500);
    }


}
