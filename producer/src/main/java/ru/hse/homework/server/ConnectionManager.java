package ru.hse.homework.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;

public class ConnectionManager extends Thread {
    private final Integer id;

    private final ArrayList<Socket> clients = new ArrayList<>();

    private final ArrayList<String> names = new ArrayList<>();

    private final Gameplay gameplay;

    private final int tn;

    private volatile Boolean stop = false;

    public ConnectionManager(int id, int tn, Gameplay gameplay) {
        this.id = id;
        this.tn = tn;
        this.gameplay = gameplay;
    }

    public void addConn(Socket s) throws IOException {
        DataInputStream in = new DataInputStream(s.getInputStream());
        DataOutputStream out = new DataOutputStream(s.getOutputStream());

        String input = in.readUTF();

        synchronized (this) {
            names.add(input);
            clients.add(s);
            gameplay.addPlayer();

            try {
                out.writeUTF(String.format("Successful connect to session id=%d", id));
            } catch (IOException e) {
                gameplay.erasePlayer(clients.size() - 1);
                names.removeLast();
                clients.removeLast().close();

                return;
            }

            sendClientsList();
        }
    }

    public synchronized void eraseConn(int ind) {
        if (clients.get(ind).isClosed()) {
            clients.remove(ind);
            names.remove(ind);
            gameplay.erasePlayer(ind);

            sendClientsList();
        }
    }

    private synchronized void sendClientsList() {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).isClosed()) {
                eraseConn(i);
                break;
            }

            try {
                DataOutputStream cur_out = new DataOutputStream(clients.get(i).getOutputStream());
                cur_out.writeUTF("List of players");
                cur_out.writeInt(clients.size());

                for (String name : names) {
                    cur_out.writeUTF(name);
                }
            } catch (IOException e) {
                if (clients.get(i).isClosed()) {
                    eraseConn(i);
                    break;
                }
            }
        }
    }

    private synchronized void sendGameProgress() {
        String[] scores = gameplay.getProgress(names);

        for (Socket client : clients) {
            try {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                out.writeUTF("Game progress:");
                out.writeInt(scores.length);
                for (String s : scores) {
                    out.writeUTF(s);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public synchronized void sendTimeExpired() {
        for (Socket client : clients) {
            try {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                out.writeUTF("Time expired");
            } catch (IOException e) {
                continue;
            }
        }
    }

    public synchronized void sendResult() {
        int winner = gameplay.getWinner();

        for (int i = 0; i < clients.size(); i++) {
            try {
                DataOutputStream out = new DataOutputStream(clients.get(i).getOutputStream());

                out.writeUTF("Game over");
                if (winner == i) {
                    out.writeUTF("You win");
                } else {
                    out.writeUTF("You lose");
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    public synchronized int getConnCount() {
        return clients.size();
    }

    public synchronized void startGame(int n, int ts) {
        for (Socket client : clients) {
            try {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                out.writeUTF(String.format("Start game session with id=%d", id));
                out.writeInt(n);
                out.writeInt(ts);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public synchronized void getAttempt(int ind) throws IOException {
        if (ind >= clients.size() || ind < 0) {
            throw new IOException();
        }

        DataOutputStream out = new DataOutputStream(clients.get(ind).getOutputStream());
        DataInputStream in = new DataInputStream(clients.get(ind).getInputStream());

        out.writeUTF("Your attempt");

        String input = in.readUTF();
        int place = in.readInt();

        if (input.length() > 1 || input.charAt(0) == '*' || place >= gameplay.getN()) {
            out.writeInt(-1);
            return;
        }

        int k = gameplay.doAttempt(input, place, ind);

        out.writeInt(k);
    }

    @Override
    public void run() {
        LocalTime prev = LocalTime.now();
        while (!stop) {
            synchronized (this) {
                for (int i = 0; i < clients.size(); i++) {
                    if (clients.get(i).isClosed()) {
                        eraseConn(i);
                    }
                }

                if (tn != 0 && LocalTime.now().isAfter(prev.plusSeconds(tn))) {
                    sendGameProgress();
                    prev = LocalTime.now();
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public synchronized void stopManager() {
        stop = true;
        for (Socket client : clients) {
            try {
                client.close();
            } catch (IOException e) {
                System.out.println("Can't close connection");
            }
        }
    }
}