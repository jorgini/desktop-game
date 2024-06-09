package ru.hse.client;

import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends Thread {
    private final String host;

    private final int port;

    private final String name;

    private volatile boolean stop = false;

    private final GameController gameController;

    private volatile String letter;

    private volatile int place;

    private volatile boolean missAttempt;

    public Client(String host, int port, String name, GameController gameController) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.gameController = gameController;
    }

    private void parseServerResponse(String response) throws IOException {
        if (response.contains("Connection dismiss")) {
            throw new IOException("Connection dismiss");
        } else if (response.contains("Successful connect")) {
            try {
                if (!gameController.sessionFind(Integer.parseInt(response.split("=")[1]))) {
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

    public synchronized void setAttempt(String letter, int place) {
        this.letter = letter;
        this.place = place;
        missAttempt = false;
    }

    public synchronized void setMissAttempt() {
        missAttempt = true;
    }

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

                if (instructions.equals("Stop connection")) {
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
            // todo show exception widget
            System.out.println("something went wrong, " + e.getMessage());
        }
    }

    public void stopGame() {
        stop = true;
        this.interrupt();
    }
}
