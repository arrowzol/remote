package org.devbar.remote.agents;

import java.io.IOException;

public interface Writer {
    void write(byte[] buffer, int start, int len) throws IOException;
    default void write(byte[] buffer) throws IOException { this.write(buffer, 0, buffer.length);}
    void closeWriter();
}
