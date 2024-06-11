package ru.hse.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * ClientApplication is class that launches gui application for user interaction with game sessions.
 */
public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("conn-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);
        scene.setFill(Color.BLACK);

        stage.setTitle("WordGame");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}