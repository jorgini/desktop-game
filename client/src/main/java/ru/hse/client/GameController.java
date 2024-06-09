package ru.hse.client;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GameController {
    @FXML
    private VBox attemptsList;

    @FXML
    private Label timerAttempt;

    @FXML
    private  Label resultAttempt;

    @FXML
    private Label warningLabel;

    @FXML
    private Label sessionInfo;

    @FXML
    private Label guessedWord;

    @FXML
    private Button submitButton;

    @FXML
    private VBox timerLayout;

    @FXML
    private VBox lenInfo;

    @FXML
    private Label attemptText;

    @FXML
    private TextField letterField;

    @FXML
    private TextField placeField;

    @FXML
    private ObservableList<ColumnConstraints> columns;

    @FXML
    private VBox playersList;

    private Client client;

    private boolean canceled = false;

    private int n;

    private AnimationTimer timer;

    private final Map<String, String> progress = new HashMap<>();

    public void initialize() {
        Pattern place = Pattern.compile("[0-9]*");
        Pattern letter = Pattern.compile("[a-zа-яё]?");
        TextFormatter<String> placeFormatter = new TextFormatter<>(change -> {
                if (place.matcher(change.getControlNewText()).matches()) {
                    return change;
                } else {
                    return null;
                }
            });
        placeField.setTextFormatter(placeFormatter);

        TextFormatter<String> letterFormatter = new TextFormatter<>(change -> {
                if (letter.matcher(change.getControlNewText()).matches()) {
                    return change;
                } else {
                    return null;
                }
            });
        letterField.setTextFormatter(letterFormatter);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public void setClient(String host, int port, String username) {
        client = new Client(host, port, username, this);

        client.start();
    }

    public boolean sessionFind(int session_id) {
        if (canceled) return false;

        Platform.runLater(() -> {
            sessionInfo.setText("Connect to session with id: " + session_id);
            System.out.println("sessionFind");
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.show();
        });
        return true;
    }

    public void startGame(int n, int ts) {
        this.n = n;
        if (ts == 0) {
            timerLayout.getChildren().add(new Label("Time to end:"));
            timerLayout.getChildren().add(new Label("Unlimited"));
            timerLayout.setVisible(true);
        } else {
            timerLayout.getChildren().add(new Label("Time to end:"));
            timerLayout.getChildren().add(new Label(formatTime(ts)));
            timerLayout.setVisible(true);

            AnimationTimer timer = new AnimationTimer() {
                final LocalTime begin = LocalTime.now();

                @Override
                public void handle(long l) {
                    int diff = (int) Duration.between(begin, LocalTime.now()).toSeconds();
                    if (diff >= 1 && diff <= ts) {
                        timerLayout.getChildren().set(1, new Label(formatTime(ts - diff)));
                    } else if (diff > ts) {
                        this.stop();
                    }
                }
            };
            timer.start();
        }

        lenInfo.getChildren().add(new Label("Length of hidden word: " + n));
        lenInfo.setVisible(true);

        guessedWord.setVisible(true);
        guessedWord.setText("*".repeat(n));

        progress.replaceAll((p, v) -> "*".repeat(n));
        printListPlayers(progress.keySet().stream().toList());
    }

    private void parseSuccess(HBox hBox, String success) {
        for (char c : success.toCharArray()) {
            Label letter = new Label("*");
            if (c == '*') {
                letter.setStyle("-fx-font-weight: bold; -fx-text-fill: #e37272; -fx-font-size: 26");
            } else {
                letter.setStyle("-fx-font-weight: bold; -fx-text-fill: #80dd00; -fx-font-size: 26");
            }
            hBox.getChildren().add(letter);
        }
    }

    public void printListPlayers(List<String> players) {
        playersList.getChildren().clear();
        for (String player : players) {
            HBox hBox = new HBox();
            Label name = new Label(player);
            HBox inner = new HBox();
            hBox.setStyle("-fx-border-width: 1; -fx-border-color: #3232d6; -fx-border-radius: 5;");
            hBox.getChildren().addAll(name, inner);
            hBox.setSpacing(20.0);
            hBox.setPadding(new Insets(5, 5, 5, 5));

            if (progress.containsKey(player)) {
                parseSuccess(inner, progress.get(player));
            } else {
                parseSuccess(hBox, "*".repeat(n));
                progress.put(player, "*".repeat(n));
            }
            playersList.getChildren().add(hBox);
        }
    }

    public void printGameProgress(List<String> progress) {
        List<String> names = new ArrayList<>();
        for (String info : progress) {
            String[] result = info.split("-");

            this.progress.put(result[0].trim(), result[1].trim());

            names.add(result[0].trim());
        }

        printListPlayers(names);
    }

    public void doAttempt() {
        attemptText.setVisible(true);
        letterField.setDisable(false);
        placeField.setDisable(false);
        submitButton.setDisable(false);
        timerAttempt.setVisible(true);
        resultAttempt.setVisible(false);

        this.timer = new AnimationTimer() {
            final LocalTime begin = LocalTime.now();

            @Override
            public void handle(long l) {
                int diff = (int) Duration.between(begin, LocalTime.now()).toSeconds();

                if (diff >= 0 && diff <= 15) {
                    timerAttempt.setText("Time to attempt - " + formatTime(15 - diff));
                } else if (diff > 15) {
                    expiredAttempt();
                    this.stop();
                }
            }
        };

        timer.start();
    }

    private void closeAttempt() {
        attemptText.setVisible(false);
        letterField.setDisable(true);
        placeField.setDisable(true);
        submitButton.setDisable(true);

        letterField.setStyle(null);
        placeField.setStyle(null);
        warningLabel.setVisible(false);
        timerAttempt.setVisible(false);
        timerAttempt.setText("");
    }

    @FXML
    protected void submitAttempt() {
        if (parseAttempt()) {
            this.timer.stop();

            synchronized (client) {
                client.setAttempt(letterField.getText(), Integer.parseInt(placeField.getText()));
                client.notify();
            }

            closeAttempt();
        } else {
            letterField.setStyle("-fx-background-color: #e37272;");
            placeField.setStyle("-fx-background-color: #e37272;");

            warningLabel.setVisible(true);
        }
    }

    protected void expiredAttempt() {
        synchronized (client) {
            client.setMissAttempt();
            client.notify();
        }

        closeAttempt();
        resultAttempt.setText("");
        resultAttempt.setVisible(false);
    }

    private boolean parseAttempt() {
        try {
            int place = Integer.parseInt(placeField.getText());
            String letter = letterField.getText();

            if (place > n || letter.length() != 1 || guessedWord.getText().charAt(place - 1) != '*') {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public void showResultAttempt(int k) {
        resultAttempt.setVisible(true);
        if (k == -1) {
            resultAttempt.setText("Wrong attempt");
        } else if (k == 0) {
            resultAttempt.setText("Wrong place, but letter exist in word");
        } else if (k == 1) {
            resultAttempt.setText("You right");
            String prev = guessedWord.getText();
            char[] word = prev.toCharArray();
            word[Integer.parseInt(placeField.getText()) - 1] = letterField.getText().charAt(0);
            guessedWord.setText(String.valueOf(word));
        } else {
            resultAttempt.setVisible(false);
        }

        addAttemptInHistory(letterField.getText(), Integer.parseInt(placeField.getText()), resultAttempt.getText());
    }

    public void printTimeExpired(String word) {
        Label title = new Label("Time expired!");
        showModal(title, word);
    }

    public void printGameOver(boolean result, String word) {
        Label title = new Label();
        if (result) {
            title.setText("You win!");
        } else {
            title.setText("You lost!");
        }
        showModal(title, word);

    }

    private void showModal(Label title, String hiddenWord) {
        Stage stage = new Stage();
        VBox vBox = new VBox();
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 24; -fx-text-fill: white;");
        Label message = new Label(String.format("Hidden word - %s . \nClose window to reconnect to new session.", hiddenWord));
        message.setStyle("-fx-text-fill: white; -fx-font-size: 18");
        vBox.getChildren().addAll(title, message);
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(20);
        vBox.setStyle("-fx-background-color: #575656;");
        vBox.setPadding(new Insets(10, 10, 10, 10));
        Scene scene = new Scene(vBox, 400, 200);
        scene.setFill(Color.BLACK);

        stage.setTitle("Game over");
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(submitButton.getScene().getWindow());
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Stage gameWindow = (Stage) submitButton.getScene().getWindow();
                gameWindow.getOnCloseRequest().handle(windowEvent);
            }
        });
    }

    private void addAttemptInHistory(String letter, int place, String result) {
        HBox hBox = new HBox();
        Label let = new Label(letter);
        let.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: white;");
        Label pla = new Label(String.valueOf(place));
        pla.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: white;");
        Label res = new Label(result);
        res.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: white;");
        if (result.contains("You right")) {
            hBox.setStyle("-fx-border-width: 1; -fx-border-color: #80dd00; -fx-border-radius: 5;");
        } else if (result.contains("Wrong place, but letter exist in word")) {
            hBox.setStyle("-fx-border-width: 1; -fx-border-color: #ffffff; -fx-border-radius: 5;");
        } else if (result.contains("Wrong attempt")) {
            hBox.setStyle("-fx-border-width: 1; -fx-border-color: #e37272; -fx-border-radius: 5;");
        }
        hBox.getChildren().addAll(let, pla, res);
        hBox.setSpacing(20.0);
        hBox.setPadding(new Insets(5, 5, 5, 5));

        attemptsList.getChildren().add(hBox);
    }

    public void cancel() {
        canceled = true;
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
        client.stopGame();

        System.out.println("canceled");
    }
}