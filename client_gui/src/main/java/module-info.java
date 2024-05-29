module ru.hse.client_gui {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.hse.client_gui to javafx.fxml;
    exports ru.hse.client_gui;
}