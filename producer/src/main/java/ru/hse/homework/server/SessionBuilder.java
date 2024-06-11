package ru.hse.homework.server;

/**
 * SessionBuilder is builder for class Session.
 */
public class SessionBuilder {
    private int id;

    private int tb;

    private int ts;

    private int tn;

    /**
     * Setter for id.
     * @param id - id.
     * @return this.
     */
    public SessionBuilder setId(int id) {
        this.id = id;
        return this;
    }

    /**
     * Setter for tb.
     * @param tb - tb.
     * @return this.
     */
    public SessionBuilder setTb(int tb) {
        this.tb = tb;
        return this;
    }

    /**
     * Setter for ts.
     * @param ts - ts.
     * @return this.
     */
    public SessionBuilder setTs(int ts) {
        this.ts = ts;
        return this;
    }

    /**
     * Setter for tn.
     * @param tn - tn.
     * @return this.
     */
    public SessionBuilder setTn(int tn) {
        this.tn = tn;
        return this;
    }

    /**
     * Build the Session instance with params.
     * @return Session instance.
     */
    public Session build() {
        return new Session(id, tb, ts, tn);
    }
}