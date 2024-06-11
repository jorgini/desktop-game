package ru.hse.client;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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

/**
 * GameController is class that handle the actions during the session and interacting with user and server by Clint
 * instance.
 */
public class GameController {
    /**
     * Widget with history of attempts.
     */
    @FXML
    private VBox attemptsList;

    /**
     * Label with time for attempt.
     */
    @FXML
    private Label timerAttempt;

    /**
     * Label with results of attempt.
     */
    @FXML
    private Label resultAttempt;

    /**
     * Label with warning message.
     */
    @FXML
    private Label warningLabel;

    /**
     * Label with info about session id.
     */
    @FXML
    private Label sessionInfo;

    /**
     * Label with guessed word.
     */
    @FXML
    private Label guessedWord;

    /**
     * Button to submit attempt.
     */
    @FXML
    private Button submitButton;

    /**
     * Widget with remaining duration of session.
     */
    @FXML
    private VBox timerLayout;

    /**
     * Widget with info about len of hidden word.
     */
    @FXML
    private VBox lenInfo;

    /**
     * Label with indication to make attempt.
     */
    @FXML
    private Label attemptText;

    /**
     * Field for guessed letter.
     */
    @FXML
    private TextField letterField;

    /**
     * Field for guessed place.
     */
    @FXML
    private TextField placeField;

    /**
     * Widget with list of players connected to session.
     */
    @FXML
    private VBox playersList;

    /**
     * Instance of Client.
     */
    private Client client;

    /**
     * Length of hidden word.
     */
    private int n;

    /**
     * Hash map with progress of each player, where progress representing as string like *+**+, where + is
     * guessed letter and * - otherwise.
     */
    private final Map<String, String> progress = new HashMap<>();

    /**
     * Initializes widget. Sets formatters for fields.
     */
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

    /**
     * Replaces the amount of second to string like "00:00".
     * @param seconds - amount of seconds.
     * @return string in time format like "00:00".
     */
    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Creates new Client instance and lunched them.
     * @param host - host on which server is launched.
     * @param port - port on which server is launched.
     * @param username - username of player.
     */
    public void setClient(String host, int port, String username) {
        client = new Client(host, port, username, this);

        client.start();
    }

    /**
     * Shows current widget with session info.
     * @param session_id - session id.
     */
    public void sessionFind(int session_id) {
        sessionInfo.setText("Connect to session with id: " + session_id);
        System.out.println("sessionFind");
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.show();
    }

    /**
     * Shows that game is start. Shows Timer with duration of session and length of hidden word.
     * @param n - length of word.
     * @param ts - duration of session.
     */
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

    /**
     * Parses progress of players into informative label.
     * @param hBox - widget where to add label.
     * @param success - progress of current player.
     */
    private void parseSuccess(HBox hBox, String success) {
        for (char c : success.toCharArray()) {
            Label letter = new Label("*");
            if (c == '*') {
                letter.setStyle("-fx-font-weight: bold; -fx-text-fill: #e37272; -fx-font-size: 26");
            } else if (c == '+') {
                letter.setStyle("-fx-font-weight: bold; -fx-text-fill: #80dd00; -fx-font-size: 26");
            } else {
                letter.setText(c + "");
                letter.setStyle("-fx-font-weight: bold; -fx-text-fill: #80dd00; -fx-font-size: 20");
            }
            hBox.getChildren().add(letter);
        }
    }

    /**
     * Prints list of players with their progress if game already start, otherwise - only usernames.
     * @param players
     */
    public void printListPlayers(List<String> players) {
        playersList.getChildren().clear();
        for (String player : players) {
            GridPane grid = new GridPane();
            Label name = new Label(player);
            HBox inner = new HBox();
            grid.setStyle("-fx-border-width: 1; -fx-border-color: #3232d6; -fx-border-radius: 5;");
            grid.setHgap(10.0);
            grid.setPadding(new Insets(5, 10, 5, 10));
            ColumnConstraints[] columns= new ColumnConstraints[]{new ColumnConstraints(), new ColumnConstraints()};
            columns[0].setPercentWidth(50.0);
            columns[0].setHalignment(HPos.CENTER);
            columns[1].setPercentWidth(50.0);
            columns[1].setHalignment(HPos.CENTER);
            grid.getColumnConstraints().addAll(columns);
            grid.add(name, 0, 0);
            grid.add(inner, 1, 0);

            if (player.equals(client.getUserName())) {
                progress.put(player, guessedWord.getText());
            }

            if (progress.containsKey(player)) {
                parseSuccess(inner, progress.get(player));
            } else {
                parseSuccess(inner, "*".repeat(n));
                progress.put(player, "*".repeat(n));
            }
            playersList.getChildren().add(grid);
        }
    }

    /**
     * Updates game progress and prints list of players.
     * @param progress
     */
    public void printGameProgress(List<String> progress) {
        List<String> names = new ArrayList<>();
        for (String info : progress) {
            String[] result = info.split("-");

            this.progress.put(result[0].trim(), result[1].trim());

            names.add(result[0].trim());
        }

        printListPlayers(names);
    }

    /**
     * Shows the indication to make attempt and fields to do it. Also starts timer for current attempt.
     */
    public void doAttempt() {
        attemptText.setVisible(true);
        letterField.setDisable(false);
        letterField.setText("");
        placeField.setDisable(false);
        placeField.setText("");
        submitButton.setDisable(false);
        timerAttempt.setVisible(true);
        resultAttempt.setVisible(false);

        AnimationTimer timer = new AnimationTimer() {
            final LocalTime begin = LocalTime.now();

            @Override
            public void handle(long l) {
                int diff = (int) Duration.between(begin, LocalTime.now()).toSeconds();

                if (diff >= 0 && diff <= 15 && timerAttempt.isVisible()) {
                    timerAttempt.setText("Time to attempt - " + formatTime(15 - diff));
                } else {
                    if (diff > 15) {
                        expiredAttempt();
                    }
                    this.stop();
                }
            }
        };

        timer.start();
    }

    /**
     * Hides the indication to make attempt and fields to do it.
     */
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

    /**
     * Handlers submitting attempt. Validate inputs and then send it to Client.
     */
    @FXML
    protected void submitAttempt() {
        if (validateAttempt()) {
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

    /**
     * Sends to client that player misses attempt.
     */
    protected void expiredAttempt() {
        synchronized (client) {
            client.setMissAttempt();
            client.notify();
        }

        closeAttempt();
        resultAttempt.setText("");
        resultAttempt.setVisible(false);
    }

    /**
     * Validates inputs on attempt.
     * @return - true if inputs valid and false - otherwise.
     */
    private boolean validateAttempt() {
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

    /**
     * Shows result of attempt received form server and call add in history.
     * @param k - response from server.
     */
    public void showResultAttempt(int k) {
        resultAttempt.setVisible(true);
        if (k == -1) {
            resultAttempt.setText("Wrong attempt");
        } else if (k == 0) {
            resultAttempt.setText("Wrong place, but letter exist");
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

    /**
     * Shows the modal with message that time of session is expired and hidden word, received form server.
     * @param word - hidden word.
     */
    public void printTimeExpired(String word) {
        showModal("Time expired!",
                String.format("Hidden word - %s . \nClose window to reconnect to new session.", word));
    }

    /**
     * Shows the modal with message that game is over and result (win/lose) and hidden word, received form server.
     * @param result - true if player win, false - otherwise.
     * @param word - hidden word.
     */
    public void printGameOver(boolean result, String word) {
        String title;
        if (result) {
            title = "You win!";
        } else {
            title = "You lost!";
        }
        showModal(title, String.format("Hidden word - %s . \nClose window to reconnect to new session.", word));
    }

    /**
     * Show the modal with message about exception on Client.
     * @param message - exception message.
     */
    public void printException(String message) {
        showModal("Something went wrong!", String.format("Error - %s", message));
    }

    /**
     * Create and show the modal with required title and message.
     * @param title - title of the modal.
     * @param message - message in the modal.
     */
    private void showModal(String title, String message) {
        Stage stage = new Stage();
        VBox vBox = new VBox();
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 24; -fx-text-fill: white;");
        Label messageLabel = new Label(String.format(message));
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18");
        vBox.getChildren().addAll(titleLabel, messageLabel);
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

    /**
     * Adds attempt in widget with history of attempts.
     * @param letter - guessed letter.
     * @param place - guessed place.
     * @param result - response from server.
     */
    private void addAttemptInHistory(String letter, int place, String result) {
        GridPane grid = new GridPane();
        Label let = new Label(letter);
        let.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: white;");
        Label pla = new Label(String.valueOf(place));
        pla.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: white;");
        Label res = new Label(result);
        res.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: white;");
        if (result.contains("You right")) {
            grid.setStyle("-fx-border-width: 1; -fx-border-color: #80dd00; -fx-border-radius: 5;");
        } else if (result.contains("Wrong place, but letter exist")) {
            grid.setStyle("-fx-border-width: 1; -fx-border-color: #ffffff; -fx-border-radius: 5;");
        } else if (result.contains("Wrong attempt")) {
            grid.setStyle("-fx-border-width: 1; -fx-border-color: #e37272; -fx-border-radius: 5;");
        }
        grid.setHgap(10.0);
        ColumnConstraints[] column = new ColumnConstraints[]{new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints()};
        column[0].setHalignment(HPos.CENTER);
        column[1].setHalignment(HPos.CENTER);
        column[2].setHalignment(HPos.CENTER);
        column[0].setPercentWidth(10.0);
        column[1].setPercentWidth(10.0);
        column[2].setPercentWidth(80.0);
        grid.getColumnConstraints().addAll(column);
        grid.setPadding(new Insets(5, 10, 5, 10));
        grid.add(let, 0, 0);
        grid.add(pla, 1, 0);
        grid.add(res, 2, 0);

        attemptsList.getChildren().add(grid);
    }

    /**
     * Cancels the game and close current window.
     */
    public void cancel() {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
        client.stopGame();

        System.out.println("canceled");
    }
}