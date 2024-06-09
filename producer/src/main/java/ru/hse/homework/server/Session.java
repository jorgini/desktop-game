package ru.hse.homework.server;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;

public class Session extends Thread {
    private final ConnectionManager manager;

    private final Gameplay gameplay;

    private volatile Boolean stop = false;

    private final int tb;

    private final int ts;

    public Session(int id, int tb, int ts, int tn) {
        this.tb = tb;
        this.ts = ts;
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

    public void setWordBase(String path) {
        gameplay.setWordBase(path);
    }

    @Override
    public void run() {
        try {
            gameplay.start();
        } catch (RuntimeException e) {
            // todo
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
//        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
//        if (ts != 0) {
//            executorService.schedule(new Interrupter(Thread.currentThread()), ts, TimeUnit.SECONDS);
//        }

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
        //executorService.close();
    }
}
