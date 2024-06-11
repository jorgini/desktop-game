package ru.hse.homework.server;

import ru.hse.homework.words.WordsReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Server is class implementation server that accepts connections and launch sessions.
 */
public class Server extends Thread {
    /**
     * Port on which server is launched.
     */
    private final int port;

    /**
     * Max amount of players in single session.
     */
    private final int m;

    /**
     * Time to start game after session has been assembled.
     */
    private final int tb;

    /**
     * Time to assemble session.
     */
    private final int tp;

    /**
     * Duration of game.
     */
    private final int ts;

    /**
     * Interval after witch players will be notified of their success.
     */
    private final int tn;

    /**
     * Length of hidden words for all sessions.
     */
    private int n = 0;

    /**
     * Hidden word for all sessions.
     */
    private String word = "";

    /**
     * File path to word base.
     */
    private String pathWordBase = WordsReader.getResourceFilename();;

    /**
     * Array with all launched sessions.
     */
    private final ArrayList<Session> sessions;

    /**
     * Server socket channel which accepts connections.
     */
    private ServerSocketChannel ss;

    /**
     * Constructor for Server.
     * @param port - port on which server is launched.
     * @param m - max amount of players in single session.
     * @param tb - time to start game after session has been assembled.
     * @param tp - time to assemble session.
     * @param ts - duration of game.
     * @param tn - interval after witch players will be notified of their success.
     */
    public Server(int port, int m, int tb, int tp, int ts, int tn) {
        this.sessions = new ArrayList<>();
        this.port = port;
        this.m = m;
        this.tp = tp;
        this.tb = tb;
        this.ts = ts;
        this.tn = tn;
    }

    /**
     * Setter for length of hidden words. When called current hidden word is disabled.
     * @param n - length of hidden word.
     */
    public synchronized void setN(Integer n) {
        this.n = n;
        this.word = "";
    }

    /**
     * Setter for current hidden words. When called length of words is disabled.
     * @param word - current hidden word.
     */
    public synchronized void setWord(String word) {
        this.word = word;
        this.n = 0;
    }

    /**
     * Setter for file path to word base.
     * @param pathWordBase - file path.
     */
    public synchronized void setPathWordBase(String pathWordBase) {
        this.pathWordBase = pathWordBase;
    }

    /**
     * Run server. Start with initialize ServerSocketChannel and then accepts the connection,
     * store them for sessions and launch sessions.
     */
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
                    if (!pathWordBase.equals(WordsReader.getResourceFilename())) {
                        cur_session.setWordBase(pathWordBase);
                    }

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

    /**
     * Shut down server. Closes ServerSocketChannel and stops all sessions.
     */
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
