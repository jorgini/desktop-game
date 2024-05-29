package ru.hse.homework.server;

public class ServerBuilder {
    private Integer port = 1234;

    private Integer m = 3;

    private Integer tb = 5;

    private Integer tp = 30;

    private Integer ts = 300;

    private Integer tn = 1;

    public ServerBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    public ServerBuilder setM(Integer m) {
        this.m = m;
        return this;
    }

    public ServerBuilder setTb(Integer tb) {
        this.tb = tb;
        return this;
    }

    public ServerBuilder setTp(Integer tp) {
        this.tp = tp;
        return this;
    }

    public ServerBuilder setTs(Integer ts) {
        this.ts = ts;
        return this;
    }

    public ServerBuilder setTn(Integer tn) {
        this.tn = tn;
        return this;
    }

    public Server build() {
        return new Server(port, m, tb, tp, ts, tn);
    }
}
