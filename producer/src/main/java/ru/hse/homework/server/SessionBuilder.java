package ru.hse.homework.server;

public class SessionBuilder {
    private int id;

    private int tb;

    private int ts;

    private int tn;

    public SessionBuilder setId(int id) {
        this.id = id;
        return this;
    }

    public SessionBuilder setTb(int tb) {
        this.tb = tb;
        return this;
    }

    public SessionBuilder setTs(int ts) {
        this.ts = ts;
        return this;
    }

    public SessionBuilder setTn(int tn) {
        this.tn = tn;
        return this;
    }

    public Session build() {
        return new Session(id, tb, ts, tn);
    }
}