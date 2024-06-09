package ru.hse.server_gui;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.hse.homework.server.Server;
import ru.hse.homework.server.ServerBuilder;

import java.io.File;
import java.util.regex.Pattern;

public class ConfigController {
    private static final String RESOURCE_FILENAME = "russian_nouns.txt";

    @FXML
    private Label textLength;

    @FXML
    private Label textWord;

    @FXML
    private TextField portField;

    @FXML
    private TextField mField;

    @FXML
    private TextField tpField;

    @FXML
    private TextField tsField;

    @FXML
    private TextField tbField;

    @FXML
    private TextField tnFiled;

    @FXML
    private TextField nField;

    @FXML
    private TextField wordField;

    @FXML
    private TextField fileField;

    @FXML
    private ComboBox<String> selector;

    @FXML
    private Label warningLabel;

    private Server server;

    private boolean isRunning = false;

    public void initialize() {
        Pattern pattern = Pattern.compile("[0-9]*");
        TextField[] fields = new TextField[]{portField, mField, tpField, tsField, tbField, tnFiled, nField};
        for (TextField field : fields) {
            TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
                if (pattern.matcher(change.getControlNewText()).matches()) {
                    return change;
                } else {
                    return null;
                }
            });
            field.setTextFormatter(textFormatter);
        }

        setDefaultFile();
    }

    private void switchEditable(boolean editable) {
        portField.setDisable(!editable);
        mField.setDisable(!editable);
        tsField.setDisable(!editable);
        tbField.setDisable(!editable);
        tnFiled.setDisable(!editable);
        tpField.setDisable(!editable);
    }

    private boolean checkInputs() {
        try {
            int port = Integer.parseInt(portField.getText());
            int m = Integer.parseInt(mField.getText());
            int ts = Integer.parseInt(tsField.getText());
            int tb = Integer.parseInt(tbField.getText());
            int tn = Integer.parseInt(tnFiled.getText());
            int tp = Integer.parseInt(tpField.getText());

            if (port < 1000 || port > 65535 || m < 1 || m > 50 || tb < 0 || tb > 10 || tp < 0 || tp > 300 || ts < 0 ||
                ts > 1000 || tn < 0 || tn > 10) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @FXML
    protected void launchServer() {
        wordField.getScene().getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                if (isRunning) {
                    server.shutDown();
                }
            }
        });

        if (!isRunning) {
            if (checkInputs()) {
                server = new ServerBuilder()
                        .setPort(Integer.parseInt(portField.getText()))
                        .setM(Integer.parseInt(mField.getText()))
                        .setTb(Integer.parseInt(tbField.getText()))
                        .setTn(Integer.parseInt(tnFiled.getText()))
                        .setTp(Integer.parseInt(tpField.getText()))
                        .setTs(Integer.parseInt(tsField.getText()))
                        .build();

                confirmWordSpecification();
                server.start();
                isRunning = true;
                switchEditable(false);
            } else {
                warningLabel.setVisible(true);
                warningLabel.setText("Incorrect values in fields");
            }
        } else {
            warningLabel.setVisible(true);
            warningLabel.setText("Server is already running");
        }
    }

    @FXML
    protected void stopServer() {
        if (isRunning) {
            isRunning = false;
            warningLabel.setVisible(false);
            server.shutDown();
            server = null;

            portField.setEditable(true);
            mField.setEditable(true);
            tpField.setEditable(true);
            tsField.setEditable(true);
            tbField.setEditable(true);
            tnFiled.setEditable(true);

            switchEditable(true);
        }
    }

    @FXML
    protected void selectWordSpecification() {
        if (selector.getSelectionModel().getSelectedItem().equals("Length of word")) {
            nField.setVisible(true);
            textLength.setVisible(true);
            wordField.setVisible(false);
            textWord.setVisible(false);
        } else if (selector.getSelectionModel().getSelectedItem().equals("Current word")) {
            nField.setVisible(false);
            textLength.setVisible(false);
            wordField.setVisible(true);
            textWord.setVisible(true);
        }
    }

    @FXML
    protected void confirmWordSpecification() {
        if (server != null) {
            server.setPathWordBase(fileField.getText());
        }

        if (selector.getSelectionModel().isEmpty()) {
            return;
        }

        if (selector.getSelectionModel().getSelectedItem().equals("Length of word")) {
            try {
                if (Integer.parseInt(nField.getText()) >= 5) {
                    System.out.println("ok");
                    nField.setStyle(null);

                    if (server != null) {
                        server.setN(Integer.parseInt(nField.getText()));
                    }
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                nField.setStyle("-fx-background-color: #e37272;");
            }
        } else if (selector.getSelectionModel().getSelectedItem().equals("Current word")) {
            if (wordField.getText().trim().length() >= 5) {
                System.out.println("ok");
                wordField.setStyle(null);
                if (server != null) {
                    server.setWord(wordField.getText());
                }
            } else {
                wordField.setStyle("-fx-background-color: #e37272;");
            }
        }
    }

    @FXML
    protected void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));

        File selected = chooser.showOpenDialog(new Stage());
        if (selected != null) {
            fileField.setText(selected.getPath());
        }
    }

    @FXML
    protected void setDefaultFile() {
        fileField.setText(RESOURCE_FILENAME);
    }

    @FXML
    protected void about() {
        Stage stage = new Stage();
        stage.setTitle("About");

        VBox root = new VBox();
        Label info = new Label("""
                This is the server configuration runner.
                You can start and stop server multiple times.
                
                Input specification:
                  - Launched port (number that indicated port on which server will be running) 1000 - 65535

                  - Count of players in session (number required to start a session) 1 - 50
                  
                  - Pause to start session (number of seconds which required to start a session after players gathering 
                    time has expired) 0 - 10
                  
                  - Players gathering time (number of seconds during which a session will gather players) 0 - 300 
                    (0 means no time limit)
                  
                  - Session duration (number of seconds during which game will take place) 0 - 1000 (0 means 
                    no time limit)
                  
                  - Notification interval (number of seconds indicates interval after witch players will be notified of 
                    their success) 0 - 10 (0 means there is no notification)
                  
                  - Word specification is optional field, that set specified word for all session, which running after 
                    confirming changes. That may be:
                        - Length of word (number of letters in a hidden words) > 4
                        - Current word (set word that will be set as a hidden word in all sessions running after 
                          confirming changes) (word should have at least 5 letters)
                """);

        info.setPrefWidth(700);
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: white; -fx-font-size: 16;");

        root.setAlignment(Pos.TOP_CENTER);
        root.paddingProperty().set(new Insets(20, 10, 10, 10));
        root.getChildren().addAll(info);
        root.setStyle("-fx-background-color: #262424");

        Scene scene = new Scene(root, 750, 550);
        scene.setFill(Color.BLACK);

        stage.setScene(scene);
        stage.show();
    }
}