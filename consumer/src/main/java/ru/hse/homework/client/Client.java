package ru.hse.homework.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
    private String host;

    private int port;

    private String name;

    public static void main(String[] args) {
        Client c = new Client();
        c.port = 3030;
        c.host = "localhost";
        c.name = "penis";

        c.start();
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(name);

            while (true) {
                String instructions = in.readUTF();

                System.out.println("input = " + instructions);
                if (instructions.contains("List of players")) {
                    int amount = in.readInt();
                    System.out.println(amount);

                    for (int i = 0; i < amount; i++) {
                        System.out.println(in.readUTF());
                    }
                } else if (instructions.contains("Start")) {
                    System.out.println("Count of letters " + in.readInt());
                } else if (instructions.contains("Game progress")) {
                    int len = in.readInt();
                    System.out.println(len);

                    for (int i = 0; i < len; i++) {
                        System.out.println(in.readUTF());
                    }
                } else if (instructions.contains("Time expired")) {
                    break;
                } else if (instructions.contains("Game over")) {
                    System.out.println(in.readUTF());
                    break;
                } else if (instructions.contains("Your attempt")) {
                    Scanner scanner = new Scanner(System.in);
                    String letter;
                    while (true) {
                        if (scanner.hasNext()) {
                            letter = scanner.nextLine();
                            break;
                        }
                    }

                    out.writeUTF(letter);
                    int place;
                    while (true) {
                        if (scanner.hasNext()) {
                            place = scanner.nextInt();
                            break;
                        }
                    }

                    out.writeInt(place);

                    int k = in.readInt();
                    System.out.println(k);
                }
            }
        } catch (IOException e) {
            System.out.println("something went wrong, " + e.getMessage());
        }
    }
}
