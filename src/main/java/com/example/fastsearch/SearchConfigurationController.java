package com.example.fastsearch;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class SearchConfigurationController {

    FSApplication app = FSApplication.getInstance();

    //

    @FXML
    private Pane namePanel;
    @FXML
    private CheckBox nameMode;
    @FXML
    private TextField NameField;

    @FXML
    private Pane FileTypePanel;
    @FXML
    private CheckBox FileTypeMode;

    @FXML
    private Pane AttrPanel;
    @FXML
    private CheckBox AttrMode;

    @FXML
    private Button FileTypeIgnoreButton;
    @FXML
    private Button FileTypeMatchButton;
    @FXML
    private Button FileTypeButton;
    @FXML
    private TextField FileTypeField;
    @FXML
    private ListView<String> FileTypeList;

    @FXML
    private TextField minSizeField;
    @FXML
    private TextField maxSizeField;

    @FXML
    private CheckBox DateCheck;
    @FXML
    private DatePicker DateSelect;
    @FXML
    private Button DateModeButton;

    @FXML
    private Button searchStartButton;


    //Actions:

    @FXML
    public void toggleName() {
        namePanel.setDisable(!nameMode.isSelected());
        app.STATE.isContainsNameAllowed = nameMode.isSelected();
        validateAll();
    }

    @FXML
    public void toggleFileType() {
        FileTypePanel.setDisable(!FileTypeMode.isSelected());
        app.STATE.isSpecifyFileTypeAllowed = FileTypeMode.isSelected();
        validateAll();
    }

    @FXML
    public void toggleAttr() {
        AttrPanel.setDisable(!AttrMode.isSelected());
        app.STATE.isAttrAllowed = AttrMode.isSelected();
        setupNumericLimitedField(minSizeField);
        setupNumericLimitedField(maxSizeField);
        validateAll();
    }

    @FXML
    public void FileTypeFieldCheck() {
        UnaryOperator<TextFormatter.Change> limitFilter = change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > 6) {
                    return null; // elutasítja a további karaktert
                }
            }
            return change;
        };

        FileTypeField.setTextFormatter(new TextFormatter<>(limitFilter));
        validateAll();
    }

    @FXML
    public void FileTypeIgnore() {
        FileTypeIgnoreButton.setDisable(true);
        FileTypeMatchButton.setDisable(false);
        app.STATE.isFileTypeModeIgnore = true;
    }

    @FXML
    public void FileTypeMatch() {
        FileTypeIgnoreButton.setDisable(false);
        FileTypeMatchButton.setDisable(true);
        app.STATE.isFileTypeModeIgnore = false;
    }

    @FXML
    public void FileTypeModify() {
        if (FileTypeList.getItems().contains(FileTypeField.getText().toLowerCase())) {
            FileTypeList.getItems().remove(FileTypeField.getText().toLowerCase());
        } else {
            FileTypeList.getItems().add(FileTypeField.getText().toLowerCase());
        }
    }

    @FXML
    public void toggleDate() {
        DateSelect.setDisable(!DateCheck.isSelected());
        DateModeButton.setDisable(!DateCheck.isSelected());
        app.STATE.isDateAllowed = DateCheck.isSelected();
        validateAll();
    }

    @FXML
    public void changeDateMode() {
        if (DateModeButton.getText().equalsIgnoreCase("Before")) {
            app.STATE.isDateConditionSpecifiyBeforeDate = true;
            //
            DateModeButton.setText("AFTER");
        } else {
            app.STATE.isDateConditionSpecifiyBeforeDate = false;
            //
            DateModeButton.setText("BEFORE");
        }
    }

    @FXML
    public void checkMinSize() {
        setupNumericLimitedField(minSizeField);
        validateAll();
    }

    @FXML
    public void checkMaxSize() {
        setupNumericLimitedField(maxSizeField);
        validateAll();
    }

    private void setupNumericLimitedField(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            // csak szám engedélyezett
            if (!newText.matches("\\d*")) {
                return null;
            }

            // max 7 karakter
            if (newText.length() > 7) {
                return null;
            }

            return change;
        };

        field.setTextFormatter(new TextFormatter<>(filter));
    }

    // VALIDATE:

    public void validateAll() {

        boolean valid = true;

        // NAME PANEL
        if (nameMode.isSelected()) {
            String text = NameField.getText();
            if (text == null || text.isBlank()) {
                valid = false;
            }
        }

        // DATE PANEL
        if (DateCheck.isSelected()) {
            if (DateSelect.getValue() == null) {
                valid = false;
            }
        }

        // ATTRIBUTE PANEL (size)
        if (AttrMode.isSelected() && !minSizeField.getText().isEmpty() && !maxSizeField.getText().isEmpty()) {
            String minText = minSizeField.getText();
            String maxText = maxSizeField.getText();

            // Mindkettőnek vagy üresnek, vagy számnak kell lennie
            if (!isValidNumeric(minText) || !isValidNumeric(maxText)) {
                valid = false;
            }

            // Ha mindkettő megvan → min <= max
            if (isValidNumeric(minText) && isValidNumeric(maxText)) {
                long min = Long.parseLong(minText);
                long max = Long.parseLong(maxText);
                if (min > max || min == max) {
                    valid = false;
                }
            }
        }

        searchStartButton.setDisable(!valid);
    }

    private boolean isValidNumeric(String s) {
        return s != null && !s.isBlank() && s.matches("\\d{1,7}");
    }


    // OTHER:

    @FXML
    public  void Back() throws IOException {
        app.loadScene("hello-view.fxml", 400, 500);
    }

    @FXML
    public  void startSearch() throws IOException{

        // confirmation
        if (!AttrMode.isSelected() && !FileTypeMode.isSelected() && !nameMode.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Search");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to start the SEARCH without any specification?\nThat could take a long time and take up a lot of resources!");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;   // the search will NOT start
            }
        }

        // getdata
        if (!NameField.getText().isBlank()) app.STATE.containsName = NameField.getText();

        for (String s : FileTypeList.getItems()) {
            System.out.println(s);
        }

        if (!FileTypeList.getItems().isEmpty()) {
            app.STATE.FileTypes.addAll(FileTypeList.getItems());
        }

        if (!DateSelect.isDisable()) {
            app.STATE.fileDateCondition = DateSelect.getValue();
        }

        if (!minSizeField.getText().isBlank()) app.STATE.FileSizeMinimum = Long.parseLong(minSizeField.getText().trim());
        if (!maxSizeField.getText().isBlank()) app.STATE.FileSizeMaximum = Long.parseLong(maxSizeField.getText().trim());

        // open search scene & start search
        app.loadScene("search.fxml", 350, 500);
    }
}
