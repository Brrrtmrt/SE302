module com.example.se {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    exports GUI;
    opens GUI to javafx.fxml;
}