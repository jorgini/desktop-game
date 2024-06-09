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

    private volatile boolean started = false;

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
            if (!names.contains(input)) {
                names.add(input);
                clients.add(s);
                gameplay.addPlayer();

                try {
                    out.writeUTF(String.format("Successful connect to session id=%d", id));
                } catch (IOException e) {
                    gameplay.erasePlayer(clients.size() - 1);
                    names.removeLast();
                    clients.removeLast().close();

                    throw e;
                }

                sendClientsList();
            } else {
                out.writeUTF("Connection dismiss");
                throw new IOException("player with this name already connect");
            }
        }
    }

    public synchronized void eraseConn(int ind) {
        try {
            clients.remove(ind).close();
        } catch (IOException e) {
            System.out.println("Connection erase: " + e.getMessage());
        }
        names.remove(ind);
        gameplay.erasePlayer(ind);

        sendClientsList();
    }

    private synchronized void sendClientsList() {
        for (int i = 0; i < clients.size(); i++) {
            try {
                DataOutputStream cur_out = new DataOutputStream(clients.get(i).getOutputStream());
                cur_out.writeUTF("List of players");
                cur_out.writeInt(clients.size());

                for (String name : names) {
                    cur_out.writeUTF(name);
                }
            } catch (IOException e) {
                if (!ping(clients.get(i))) {
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
                System.out.println("Can't send game progress: " + e.getMessage());
            }
        }
    }

    public synchronized void sendTimeExpired() {
        String word = gameplay.getHiddenWord();
        for (Socket client : clients) {
            try {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                out.writeUTF("Time expired");
                out.writeUTF(word);
            } catch (IOException e) {
                System.out.println("Can't send time expired: " + e.getMessage());
            }
        }
    }

    public synchronized void sendResult() {
        int winner = gameplay.getWinner();
        String word = gameplay.getHiddenWord();

        for (int i = 0; i < clients.size(); i++) {
            try {
                DataOutputStream out = new DataOutputStream(clients.get(i).getOutputStream());

                out.writeUTF("Game over");
                out.writeUTF(word);
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
        started = true;
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

        // hardcore
        clients.get(ind).setSoTimeout(20000);
        String input = in.readUTF();
        clients.get(ind).setSoTimeout(0);

        if (input.equals("Miss")) {
            throw new IOException("client miss attempt");
        }

        int place = in.readInt();

        if (input.length() > 1 || input.charAt(0) == '*' || place >= gameplay.getN()) {
            out.writeInt(-1);
            return;
        }

        int k = gameplay.doAttempt(input, place, ind);

        out.writeInt(k);
    }

    private synchronized boolean ping(Socket client) {
        try {
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            DataInputStream in = new DataInputStream(client.getInputStream());

            out.writeUTF("Ping");
            client.setSoTimeout(1000);
            String input = in.readUTF();
            client.setSoTimeout(0);
            return input.equals("Pong");
        } catch (IOException  e) {
            System.out.println("Dismiss on ping: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void run() {
        LocalTime prev = LocalTime.now();
        while (!stop) {
            synchronized (this) {
                for (int i = 0; i < clients.size(); i++) {
                    if (!ping(clients.get(i))) {
                        eraseConn(i);
                    }
                }

                if (started && tn != 0 && LocalTime.now().isAfter(prev.plusSeconds(tn))) {
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
            try (DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
                out.writeUTF("Stop connection");
                client.close();
            } catch (IOException e) {
                System.out.println("Can't close connection");
            }
        }
    }
}