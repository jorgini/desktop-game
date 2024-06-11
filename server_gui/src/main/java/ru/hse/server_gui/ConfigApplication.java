package ru.hse.server_gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * ConfigApplication is class to launch server configuration gui as a widget.
 */
public class ConfigApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ConfigApplication.class.getResource("config-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 600);
        scene.setFill(Color.BLACK);

        stage.setTitle("Server configuration");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}