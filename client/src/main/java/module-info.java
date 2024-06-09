module ru.hse.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.management;
    requires java.sql;


    opens ru.hse.client to javafx.fxml;
    exports ru.hse.client;
}