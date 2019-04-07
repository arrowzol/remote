package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

/** An agent abstracts pairs of communications channels.
 *
 * Agents come in pairs, one on each side of the client-server system.  When an agent is created on one side, a
 * corresponding agent is created on the other side and communications between the two is done by calling the writer's
 * {@link Writer#write(byte[], int, int)} method, which is delivered by calling the {@link #consume(byte[], int, int)}
 * method on the other side.  Bytes are not guaranteed to be delivered without being split into multiple consume events.
 */
public interface Agent {
    /** Initialize the agent.
     *
     * After initialization, the agent can start operating.
     *
     * @param multiplexTunnel The multiplexTunnel in charge, can be null if there is none.
     * @param writer The writer this agent should use.
     * @param isServer This agent was created in the server.
     * @param first This agent was the first of the pair created.
     */
    void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first);

    default boolean needBuffering() { return false; }

    /** Consume bytes sent from the remote agent.
     *
     * Deliver bytes sent from the remote agent.
     */
    void consume(Bytes bytes);

    /** Close the agent.
     *
     * This will be closed if communications can't be continued.
     */
    void closeAgent();
}
