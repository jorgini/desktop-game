package ru.hse.client;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.regex.Pattern;

public class ConnController {
    @FXML
    public TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField usernameField;

    @FXML
    private Label warningLabel;

    @FXML
    private Button cancel;

    public void initialize() {
        Pattern pattern = Pattern.compile("[0-9]*");
        TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
            if (pattern.matcher(change.getControlNewText()).matches()) {
                return change;
            } else {
                return null;
            }
        });
        portField.setTextFormatter(textFormatter);
    }

    private boolean validateInput() {
        if (hostField.getText().trim().isEmpty() || usernameField.getText().trim().isEmpty()) {
            return false;
        }

        try {
            if (Integer.parseInt(portField.getText()) < 1000 || Integer.parseInt(portField.getText()) > 65535) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void switchValid(boolean valid) {
        if (valid) {
            hostField.setStyle(null);
            portField.setStyle(null);
            usernameField.setStyle(null);
            warningLabel.setVisible(false);
        } else {
            hostField.setStyle("-fx-background-color: #e37272; -fx-text-fill: white;");
            portField.setStyle("-fx-background-color: #e37272; -fx-text-fill: white;");
            usernameField.setStyle("-fx-background-color: #e37272; -fx-text-fill: white;");
            warningLabel.setVisible(true);
        }
    }

    private void switchLockFields(boolean lock) {
        hostField.setDisable(lock);
        portField.setDisable(lock);
        usernameField.setDisable(lock);
    }

    private void showWindow() {
        Stage window = (Stage) hostField.getScene().getWindow();
        window.show();
        cancelGame();
    }

    public void hideWindow() {
        Stage window = (Stage) hostField.getScene().getWindow();
        window.hide();
    }

    private void cancelGame() {
        switchLockFields(false);
        cancel.setVisible(false);
        cancel.setOnMouseClicked(null);
    }

    @FXML
    protected void startGame() throws IOException {
        boolean valid = validateInput();
        switchValid(valid);

        if (valid) {
            Stage gameStage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("game-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1080, 720);
            scene.setFill(Color.BLACK);

            gameStage.setTitle("WordGame");
            gameStage.setScene(scene);

            GameController gameController = fxmlLoader.getController();
            gameController.setClient(hostField.getText(), Integer.parseInt(portField.getText()), usernameField.getText());

            switchLockFields(true);
            cancel.setVisible(true);

            cancel.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    gameController.cancel();
                    cancelGame();
                }
            });

            gameStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent windowEvent) {
                    showWindow();
                    gameController.cancel();
                }
            });

            gameStage.setOnShown(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent windowEvent) {
                    hideWindow();
                }
            });
        }
    }

    @FXML
    protected void showAbout() {
        Stage stage = new Stage();
        stage.setTitle("About");

        VBox root = new VBox();
        Label info = new Label("""
                This is the Word Game application.
                You can play game multiple times. Please find out the host and port on which the server is running
                before starting.
           
                Input specification:
                  - Launched host (host on which server will be running) (not empty)
                  
                  - Launched port (number that indicated port on which server will be running) 1000 - 65535

                  - Username (your username, that other players will see during the game) (at least 1 symbol)
                
                Rules of game:
                Server make a word, that you and other players will guess. All player make attempts in turn to 
                identify letter and place where it is in hidden word. Game continue until someone guess the whole word
                ot game time is expired. If time expired no one wins.
                
                You can track the timer and progress of other players.
                Good luck and have fun!
                
                Created by Belikov Georgiy. 
                """);

        info.setPrefWidth(700);
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: white; -fx-font-size: 16;");

        root.setAlignment(Pos.TOP_CENTER);
        root.paddingProperty().set(new Insets(20, 10, 10, 10));
        root.getChildren().addAll(info);
        root.setStyle("-fx-background-color: #262424");

        Scene scene = new Scene(root, 750, 450);
        scene.setFill(Color.BLACK);

        stage.setScene(scene);
        stage.show();
    }
}
