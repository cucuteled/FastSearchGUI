module com.example.fastsearch {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens com.example.fastsearch to javafx.fxml;
    exports com.example.fastsearch;
}