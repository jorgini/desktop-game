package ru.hse.homework.server;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Server server = new ServerBuilder()
                .setPort(3030)
                .setM(1)
                .setTb(1)
                .setTn(1)
                .setTp(15)
                .build();

        server.start();

        Thread.sleep(300 * 1000L);

        server.shutDown();
    }
}