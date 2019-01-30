package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;

public interface Agent {
    /** Register a writer.
     *
     * Called before anything else to register a writer to send bytes to the remote agent.
     */
    void registerWriter(Writer writer);

    /** Consume bytes from the remote agent.
     *
     * Deliver bytes from the remote agent.
     */
    void consume(byte[] buffer, int off, int len);

    /** Close the agent.
     *
     * This agent must be closed because the communication has been broken.
     */
    void closeAgent();

    default void go(MultiplexTunnel multiplexTunnel, boolean isServer, boolean first) { }
}
