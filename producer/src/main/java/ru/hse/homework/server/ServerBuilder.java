package ru.hse.homework.server;

/**
 * ServerBuilder is builder for class Server.
 */
public class ServerBuilder {
    private Integer port = 1234;

    private Integer m = 3;

    private Integer tb = 5;

    private Integer tp = 30;

    private Integer ts = 300;

    private Integer tn = 1;

    /**
     * Setter for port filed.
     * @param port - port.
     * @return this.
     */
    public ServerBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    /**
     * Setter for m filed.
     * @param m - m.
     * @return this.
     */
    public ServerBuilder setM(Integer m) {
        this.m = m;
        return this;
    }

    /**
     * Setter for tb filed.
     * @param tb - tb.
     * @return this.
     */
    public ServerBuilder setTb(Integer tb) {
        this.tb = tb;
        return this;
    }

    /**
     * Setter for tp filed.
     * @param tp - tp.
     * @return this.
     */
    public ServerBuilder setTp(Integer tp) {
        this.tp = tp;
        return this;
    }

    /**
     * Setter for ts filed.
     * @param ts - ts.
     * @return this.
     */
    public ServerBuilder setTs(Integer ts) {
        this.ts = ts;
        return this;
    }

    /**
     * Setter for tn filed.
     * @param tn - tn.
     * @return this.
     */
    public ServerBuilder setTn(Integer tn) {
        this.tn = tn;
        return this;
    }

    /**
     * Builds the Server instance.
     * @return Server instance with set params.
     */
    public Server build() {
        return new Server(port, m, tb, tp, ts, tn);
    }
}
