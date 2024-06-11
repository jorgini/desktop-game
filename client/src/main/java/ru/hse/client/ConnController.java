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

/**
 * ConnController is class that implementing connection widget. There user enter host, port and username. He can
 * also see about information.
 */
public class ConnController {
    /**
     * Field to enter host where server is launched.
     */
    @FXML
    public TextField hostField;

    /**
     * Field to enter port where server is launched.
     */
    @FXML
    private TextField portField;

    /**
     * Field to enter username of player.
     */
    @FXML
    private TextField usernameField;

    /**
     * Label with warning.
     */
    @FXML
    private Label warningLabel;

    /**
     * Button that cancels connection to session.
     */
    @FXML
    private Button cancel;

    /**
     * Initializes widget. Sets formatter for port filed.
     */
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

    /**
     * Validates input. Check port value and that host and username isn't empty.
     * @return - true if inputs valid, false - otherwise.
     */
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

    /**
     * Changes view of filed. If inputs isn't valid, then show warning and paint the fields in red.
     * @param valid - is inputs valid or not.
     */
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

    /**
     * Changes editable of fields.
     * @param lock - true if fields shouldn't be editable and false - otherwise.
     */
    private void switchLockFields(boolean lock) {
        hostField.setDisable(lock);
        portField.setDisable(lock);
        usernameField.setDisable(lock);
    }

    /**
     * Shows current window and cancel game if it stars.
     */
    private void showWindow() {
        Stage window = (Stage) hostField.getScene().getWindow();
        window.show();
        cancelGame();
    }

    /**
     * Hides current window.
     */
    public void hideWindow() {
        Stage window = (Stage) hostField.getScene().getWindow();
        window.hide();
    }

    /**
     * Changes editable of fields and hide cancel button.
     */
    private void cancelGame() {
        switchLockFields(false);
        cancel.setVisible(false);
        cancel.setOnMouseClicked(null);
    }

    /**
     * Starts game searching. Initializes new game widget but, doesn't show it until game will be found.
     * Sets events on close game window to return current window and cancel game if cancel button is pressed.
     * @throws IOException - may throws by fxml.load().
     */
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

    /**
     * Shows about window with information about the game.
     */
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
                identify letter and place where it is in hidden word. You will have 15 seconds to attempt. 
                Game continue until someone guess the whole word or game time is expired. If time expired last player 
                has last opportunity to guess the letter and place. After his attempt if no one guessed whole word then 
                no one wins.
                
                You can track the timer and progress of other players. Also in right you can track your previous 
                attempts.
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

        Scene scene = new Scene(root, 750, 500);
        scene.setFill(Color.BLACK);

        stage.setScene(scene);
        stage.show();
    }
}
