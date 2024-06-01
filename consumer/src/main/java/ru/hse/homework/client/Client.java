package ru.hse.homework.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client extends Thread {
    private String host;

    private int port;

    private String name;

    private volatile Boolean stop = false;

    private static class Attempt {
        String letter;

        int place;
    }

    public static void main(String[] args) {
        Client c = new Client();
        c.port = 3030;
        c.host = "localhost";
        c.name = "penis";

        c.start();
    }

    public synchronized void stopGame() {
        stop = true;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(name);
            int n = 5, ts = 300;

            while (true) {
                synchronized (this) {
                    if (stop) {
                        break;
                    }
                }

                String instructions = in.readUTF();

                System.out.println(instructions);
                if (instructions.contains("List of players")) {
                    List<String> players = readArrayString(in);

                    printListPlayers(players);
                } else if (instructions.contains("Start")) {
                    n = in.readInt();
                    ts = in.readInt();
                    System.out.println("Count of letters " + n);
                    System.out.println("timer = " + ts + "second");

                    startGame(n, ts);
                } else if (instructions.contains("Game progress")) {
                    List<String> progress = readArrayString(in);

                    printProgress(progress);
                } else if (instructions.contains("Time expired")) {
                    System.out.println("Time expired");

                    priTimeExpired();
                    break;
                } else if (instructions.contains("Game over")) {
                    String result = in.readUTF();
                    System.out.println(result);

                    printGameOver(result);
                    break;
                } else if (instructions.contains("Your attempt")) {
                    while (true) {
                        synchronized (this) {
                            if (stop) {
                                break;
                            }
                        }

                        try {
                            Attempt attempt = doStep(n);
                            out.writeUTF(attempt.letter);
                            out.writeInt(attempt.place);

                            int k = in.readInt();
                            System.out.println(k);

                            break;
                        } catch (IllegalArgumentException e) {
                            System.out.println("wrong input, try again");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("something went wrong, " + e.getMessage());
        }
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

    private void printListPlayers(List<String> players) {

    }

    private void startGame(int n, int ts) {

    }

    private void printProgress(List<String> progress) {

    }

    private void priTimeExpired() {

    }

    private void printGameOver(String message) {

    }

    private Attempt doStep(int n) {
        Scanner scanner = new Scanner(System.in);
        Attempt attempt = new Attempt();

        while (true) {
            if (scanner.hasNext()) {
                attempt.letter = scanner.nextLine();
                break;
            }
        }

        while (true) {
            if (scanner.hasNext()) {
                attempt.place = scanner.nextInt();
                break;
            }
        }

        if (attempt.letter.length() > 1 || attempt.place > n) {
            throw new IllegalArgumentException();
        }

        attempt.place--;
        return attempt;
    }
}
