package org.devbar.remote.agents;

public abstract class BasicAgent implements Agent {
    protected Writer writer;
    protected boolean isClosed;

    @Override
    public void registerWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void closeAgent() {
        writer.closeWriter();
        isClosed = true;
    }
}
