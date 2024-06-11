package ru.hse.homework.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Connection manager is implementation of manager, that stores connection, check their keep alive and send messages.
 */
public class ConnectionManager extends Thread {
    /**
     * Identifier of the session the manager belongs to
     */
    private final Integer id;

    /**
     * Array of sockets.
     */
    private final ArrayList<Socket> clients = new ArrayList<>();

    /**
     * Array of usernames.
     */
    private final ArrayList<String> names = new ArrayList<>();

    /**
     * Instance of gameplay with which session is launched.
     */
    private final Gameplay gameplay;

    /**
     * Flag indicates that game is start.
     */
    private volatile boolean started = false;

    /**
     * Interval after witch players will be notified of their success.
     */
    private final int tn;

    /**
     * Flag that indicates end of session.
     */
    private volatile Boolean stop = false;

    /**
     * Constructor for ConnectionManager
     * @param id - identifier of session.
     * @param tn - interval after witch players will be notified of their success.
     * @param gameplay - instance of gameplay with which session is launched.
     */
    public ConnectionManager(int id, int tn, Gameplay gameplay) {
        this.id = id;
        this.tn = tn;
        this.gameplay = gameplay;
    }

    /**
     * Method to add new connection to session. Checks for unique username.
     * @param s - socket with new connection.
     * @throws IOException - if connections failed or players with this username already exist in current session.
     */
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

    /**
     * Method to erase some connection. Closes it and send new clients list to players.
     * @param ind - index of client that needs to erase.
     */
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

    /**
     * Method to send list of clients to all players. If some connection closed, call erase and recurse send actual
     * list again.
     */
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

    /**
     * Method to send progress of game to players. Get progress form Gameplay instance.
     */
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

    /**
     * Method to send that time is expired and hidden word to players.
     */
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

    /**
     * Method to send result of game. Get winner and hidden word from gameplay.
     */
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
                System.out.println("Cant send result:" + e.getMessage());
            }
        }
    }

    /**
     * Getter for count of connection.
     * @return count of keep alive connections.
     */
    public synchronized int getConnCount() {
        return clients.size();
    }

    /**
     * Method that send clients message that game is start.
     * @param n - length of hidden word.
     * @param ts - duration of session.
     */
    public synchronized void startGame(int n, int ts) {
        started = true;
        for (Socket client : clients) {
            try {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                out.writeUTF(String.format("Start game session with id=%d", id));
                out.writeInt(n);
                out.writeInt(ts);
            } catch (IOException e) {
                System.out.println("Cant send start game session: " + e.getMessage());
            }
        }
    }

    /**
     * Method that request attempt from current player. If attempt doesn't come for 20 seconds,
     * socket closes and attempt miss.
     * @param ind index of player, that should do attempt.
     * @throws IOException if connection missed or incorrect index of client.
     */
    public synchronized void getAttempt(int ind) throws IOException {
        if (ind >= clients.size() || ind < 0) {
            throw new IOException("Incorrect index of client");
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

    /**
     * Method to ping the current connection.
     * @param client socket to ping.
     * @return true - if connection is keep alive and false - otherwise.
     */
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

    /**
     * Run ConnectionManager. Checks in timeout keep alive of connections and send game progress if it is necessary.
     */
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

    /**
     * Method to stop the connection manager and close all sockets.
     */
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