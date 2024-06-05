module ru.hse.server_gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires producer;
    requires java.desktop;

    opens ru.hse.server_gui to javafx.fxml;
    exports ru.hse.server_gui;
}