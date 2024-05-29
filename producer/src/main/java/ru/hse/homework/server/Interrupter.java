package ru.hse.homework.server;

public class Interrupter extends Thread {
    private final Thread target;

    public Interrupter(Thread target) {
        this.target = target;
    }

    @Override
    public void run() {
        target.interrupt();
    }
}
