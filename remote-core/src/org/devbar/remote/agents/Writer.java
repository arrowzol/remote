package org.devbar.remote.agents;

import java.io.IOException;

/** This is given to an {@link Agent} to allow it to send messages to its sibling agent.
 */
public interface Writer {
    void write(byte[] buffer, int start, int len) throws IOException;
    default void write(byte[] buffer) throws IOException { this.write(buffer, 0, buffer.length);}
    void closeWriter();
}
