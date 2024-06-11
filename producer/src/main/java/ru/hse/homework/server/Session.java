package ru.hse.homework.server;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;

/**
 * Session is class implementing game session. It calls instructions for Connection manger
 * in the order determined by the rules of the game.
 */
public class Session extends Thread {
    /**
     * Connection manager that stores all connections, check their keep alive and send messages.
     */
    private final ConnectionManager manager;

    /**
     * Instance of game logic. Checks players attempts and show results.
     */
    private final Gameplay gameplay;

    /**
     * Flag indicates end of session.
     */
    private volatile Boolean stop = false;

    /**
     * Time to start game after session has been assembled.
     */
    private final int tb;

    /**
     * Duration of session.
     */
    private final int ts;

    /**
     * Constructor for Session.
     * @param id - identifier of session.
     * @param tb - time to start game after session has been assembled.
     * @param ts - duration of session.
     * @param tn - interval after witch players will be notified of their success.
     */
    public Session(int id, int tb, int ts, int tn) {
        this.tb = tb;
        this.ts = ts;
        this.gameplay = new Gameplay();
        this.manager = new ConnectionManager(id, tn, gameplay);

        manager.start();
    }

    /**
     * Method to stop session and connection manger.
     */
    public void stopSession() {
        manager.stopManager();
        stop = true;
    }

    /**
     * Method to add new client to session. Redirects to connection manager.
     * @param s - socket to add.
     * @throws IOException if client don't added.
     */
    public void addClient(Socket s) throws IOException {
        manager.addConn(s);
    }

    /**
     * Getter for amount of players in session.
     * @return count of clients.
     */
    public int getCountOfClients() {
        return manager.getConnCount();
    }

    /**
     * Setter for current hidden word. Redirects to Gameplay instance.
     * @param word - current hidden word.
     */
    public void setWord(String word) {
        gameplay.setHiddenWord(word);
    }

    /**
     * Setter for length of hidden word. Redirects to Gameplay instance.
     * @param n - length of hidden word.
     */
    public void setLenWord(int n) {
        gameplay.setN(n);
    }

    /**
     * Setter for file path to word base. Redirects to Gameplay instance.
     * @param path - file path.
     */
    public void setWordBase(String path) {
        gameplay.setWordBase(path);
    }

    /**
     * Run the session. Guessed the word, wait tb seconds to start. Then ask players for attempts until there is no
     * players or session will be stopped or duration of session is gone.
     * Send results to clients and stop connection manager.
     */
    @Override
    public void run() {
        try {
            gameplay.start();
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            manager.stopManager();
            return;
        }

        if (tb != 0) {
            try {
                Thread.sleep(tb * 1000L);
            } catch (InterruptedException e) {
                System.out.println("something went wrong on pause:" + e.getMessage());
            }
        }

        manager.startGame(gameplay.getN(), ts);
        LocalTime start = LocalTime.now();

        while (!stop && manager.getConnCount() != 0 && (ts == 0 || LocalTime.now().isBefore(start.plusSeconds(ts)))) {
            int player = gameplay.getNextPlayerStep();

            try {
                manager.getAttempt(player);
            } catch (IOException e) {
                System.out.println("Error occurred on waiting for attempt: " + e.getMessage());
                continue;
            }

            if (gameplay.isFinished()) {
                stop = true;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Interrupt: " + e.getMessage());
            }
        }

        if (!gameplay.isFinished()) {
            System.out.println("Time expired");
            manager.sendTimeExpired();
        } else {
            System.out.println("Game over");
            manager.sendResult();
        }

        manager.stopManager();
    }
}
