package ru.hse.homework.server;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Session extends Thread {
    private final ConnectionManager manager;

    private final Gameplay gameplay;

    private volatile Boolean stop = false;

    private final int tb;

    private final int ts;

    private final int tn;

    public Session(int id, int tb, int ts, int tn) {
        this.tb = tb;
        this.ts = ts;
        this.tn = tn;
        this.gameplay = new Gameplay();
        this.manager = new ConnectionManager(id, tn, gameplay);

        manager.start();
    }

    public void stopSession() {
        manager.stopManager();
        stop = true;
    }

    public void addClient(Socket s) throws IOException {
        manager.addConn(s);
    }

    public int getCountOfClients() {
        return manager.getConnCount();
    }

    public void setWord(String word) {
        gameplay.setHiddenWord(word);
    }

    public void setLenWord(int n) {
        gameplay.setN(n);
    }

    @Override
    public void run() {
        gameplay.start();

        if (tb != 0) {
            try {
                Thread.sleep(tb * 1000L);
            } catch (InterruptedException e) {
                System.out.println("something went wrong on pause:" + e.getMessage());
            }
        }

        manager.startGame(gameplay.getN(), ts);
        LocalTime start = LocalTime.now();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        if (ts != 0) {
            executorService.schedule(new Interrupter(Thread.currentThread()), ts, TimeUnit.SECONDS);
        }

        while (!stop && (ts == 0 || LocalTime.now().isBefore(start.plusSeconds(ts)))) {
            int player = gameplay.getNextPlayerStep();

            try {
                manager.getAttempt(player);
            } catch (IOException e) {
                continue;
            }

            if (gameplay.isFinished()) {
                stop = true;
            }
        }

        if (!gameplay.isFinished()) {
            manager.sendTimeExpired();
        } else {
            manager.sendResult();
        }

        manager.stopManager();
        executorService.close();
    }
}
