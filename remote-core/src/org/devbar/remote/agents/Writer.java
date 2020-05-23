package org.devbar.remote.agents;

import org.devbar.remote.utils.Bytes;

import java.io.IOException;

/** This is given to an {@link Agent} to allow it to send messages to its sibling agent.
 */
public interface Writer {
    boolean write(byte[] buffer, int start, int len) throws IOException;
    default boolean write(byte[] buffer) throws IOException { return this.write(buffer, 0, buffer.length);}

    /** The agent can call this to close the channel.  Both agents will be closed after this call is made.
     *
     * @param reason The reason for closing.  Negative values are reserved for I/O issues.
     */
    void closeWriter(int reason);
}
