package ru.hse.homework.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalTime;
import java.util.ArrayList;

public class Server extends Thread {
    private final int port;

    private final int m;

    private final int tb;

    private final int tp;

    private final int ts;

    private final int tn;

    private int n = 0;

    private String word = "";

    private final ArrayList<Session> sessions;

    private ServerSocketChannel ss;

    public Server(int port, int m, int tb, int tp, int ts, int tn) {
        this.sessions = new ArrayList<>();
        this.port = port;
        this.m = m;
        this.tp = tp;
        this.tb = tb;
        this.ts = ts;
        this.tn = tn;
    }

    public synchronized void setN(Integer n) {
        this.n = n;
        this.word = "";
    }

    public synchronized void setWord(String word) {
        this.word = word;
        this.n = 0;
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            ss = serverSocket;
            System.out.println("Server successfully launched on port " + port);
            int session_id = 0;

            while (true) {
                Session cur_session = new SessionBuilder()
                        .setId(++session_id)
                        .setTb(tb)
                        .setTs(ts)
                        .setTn(tn)
                        .build();
                sessions.add(cur_session);

                LocalTime start = LocalTime.now();

                while (cur_session.getCountOfClients() < m && (tp == 0 || LocalTime.now().isBefore(start.plusSeconds(tp)))) {
                    SocketChannel channel = ss.accept();
                    if (channel == null) {
                        continue;
                    }

                    try {
                        cur_session.addClient(channel.socket());
                    } catch (IOException e) {
                        System.out.println("Accept new client, but error occurred on adding to session: " + e.getMessage());
                    }
                }

                if (cur_session.getCountOfClients() == 0) {
                    cur_session.stopSession();
                    sessions.removeLast();
                    continue;
                }

                synchronized (this) {
                    if (n >= 5) {
                        cur_session.setLenWord(n);
                    } else if (!word.isEmpty()) {
                        cur_session.setWord(word);
                    }
                }

                cur_session.start();
            }
        } catch (IOException e) {
            System.out.println("Server shutdown");
        }
    }

    public void shutDown() {
        try {
            ss.close();

            for (Session s : sessions) {
                s.stopSession();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
