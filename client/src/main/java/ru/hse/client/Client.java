package ru.hse.client;

import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Client is class that implementing consumer connection and interaction with the server.
 */
public class Client extends Thread {
    /**
     * Host on which server is launched.
     */
    private final String host;

    /**
     * Port on which server is launched.
     */
    private final int port;

    /**
     * Username of player.
     */
    private final String name;

    /**
     * Flag that indicates that game is over.
     */
    private volatile boolean stop = false;

    /**
     * Instance of GameController to show some action in gui.
     */
    private final GameController gameController;

    /**
     * Guessed letter on each attempt.
     */
    private volatile String letter;

    /**
     * Guessed place on each attempt.
     */
    private volatile int place;

    /**
     * Flag that indicates that player misses his attempt.
     */
    private volatile boolean missAttempt;

    /**
     * Constructor for Client.
     *
     * @param host           - host on which server is launched.
     * @param port           - port on which server is launched.
     * @param name           - username of player.
     * @param gameController - instance of GameController to show some action in gui.
     */
    public Client(String host, int port, String name, GameController gameController) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.gameController = gameController;
    }

    /**
     * Parses server response on connection.
     *
     * @param response - response of server.
     * @throws IOException - throws if connection dismiss due server, client or if session id in response is invalid.
     */
    private void parseServerResponse(String response) throws IOException {
        if (response.contains("Connection dismiss")) {
            throw new IOException("Connection dismiss");
        } else if (response.contains("Successful connect")) {
            try {
                if (!stop) {
                    Platform.runLater(() ->gameController.sessionFind(Integer.parseInt(response.split("=")[1])));
                } else {
                    throw new IOException("Connection canceled due client");
                }
                System.out.println(response);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid session id");
            }
        } else {
            throw new IOException("Invalid response");
        }
    }

    /**
     * Setter for guessed letter and place in attempt.
     *
     * @param letter - guessed letter.
     * @param place  - guessed place.
     */
    public synchronized void setAttempt(String letter, int place) {
        this.letter = letter;
        this.place = place;
        missAttempt = false;
    }

    /**
     * Setter for flag that player misses his attempt.
     */
    public synchronized void setMissAttempt() {
        missAttempt = true;
    }

    /**
     * Getter for username.
     * @return - username.
     */
    public String getUserName() {
        return name;
    }

    /**
     * Reads list of strings from server and returned it.
     *
     * @param in - input stream.
     * @return - list of strings sent from server.
     * @throws IOException - trows if I/O errors occur.
     */
    private List<String> readArrayString(DataInputStream in) throws IOException {
        int len = in.readInt();
        System.out.println(len);
        List<String> array = new ArrayList<>();

        for (int i = 0; i < len; i++) {
            array.add(in.readUTF());
            System.out.println(array.getLast());
        }

        return array;
    }

    /**
     * Running Client. Connect to server, send username, check response and then wait some instructions until
     * game is over. Every instruction is followed by action like call gui method or break loop.
     */
    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(name);

            String response = in.readUTF();

            parseServerResponse(response);

            while (!stop) {
                String instructions = in.readUTF();

                if (!instructions.equals("Ping")) {
                    System.out.println(instructions);
                }

                if (instructions.contains("Stop connection")) {
                    throw new IOException("Game canceled due server");

                } else if (instructions.contains("List of players")) {
                    List<String> players = readArrayString(in);

                    Platform.runLater(() -> gameController.printListPlayers(players));

                } else if (instructions.contains("Start")) {
                    int n = in.readInt();
                    int ts = in.readInt();
                    System.out.println("Count of letters " + n);
                    System.out.println("timer = " + ts + "second");

                    Platform.runLater(() -> gameController.startGame(n, ts));

                } else if (instructions.contains("Game progress")) {
                    List<String> progress = readArrayString(in);

                    Platform.runLater(() -> gameController.printGameProgress(progress));

                } else if (instructions.contains("Time expired")) {
                    String word = in.readUTF();

                    Platform.runLater(() -> gameController.printTimeExpired(word));
                    break;

                } else if (instructions.contains("Game over")) {
                    String word = in.readUTF();
                    String result = in.readUTF();
                    System.out.println(result);

                    Platform.runLater(() -> gameController.printGameOver(result.contains("You win"), word));
                    break;

                } else if (instructions.contains("Your attempt")) {
                    gameController.doAttempt();
                    try {
                        synchronized (this) {
                            this.wait();

                            if (missAttempt) {
                                out.writeUTF("Miss");
                                continue;
                            }

                            out.writeUTF(letter);
                            out.writeInt(place - 1);

                            int k = in.readInt();

                            Platform.runLater(() -> gameController.showResultAttempt(k));
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted on waiting for attempt");
                    }

                } else if (instructions.contains("Ping")) {
                    out.writeUTF("Pong");
                }
            }
        } catch (IOException e) {
            System.out.println("something went wrong, " + e.getMessage());
            Platform.runLater(() -> gameController.printException(e.getMessage()));
        }
    }

    /**
     * Stops the game. Sets flag and interrupt game thread.
     */
    public void stopGame() {
        stop = true;
        this.interrupt();
    }
}
