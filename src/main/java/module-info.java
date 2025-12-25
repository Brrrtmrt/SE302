module com.example.se {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires jdk.localedata;

    exports GUI;
    opens GUI to javafx.fxml;
}