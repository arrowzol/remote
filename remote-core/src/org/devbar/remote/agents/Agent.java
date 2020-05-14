package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

/** An agent abstracts pairs of communications channels.
 *
 * Agents come in pairs, one on each side of the client-server system.  When an agent is created on one side, a
 * corresponding agent is created on the other side and communications between the two is done by calling the writer's
 * {@link Writer#write(byte[], int, int)} method, which is delivered by calling the {@link #consume(Bytes)}
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
     * @param isFirst This agent was the first of the pair created.
     */
    void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean isFirst);

    default boolean needBuffering() { return false; }

    /** Consume bytes sent from the remote agent.
     *
     * Deliver bytes sent from the remote agent.
     */
    void consume(Bytes bytes);

    /** Close the agent.
     *
     * This will be called if communications can't be continued.  This is called after all communication is closed,
     * and should not call writer.closeWriter in response.
     *
     * @param reason The reason passed by the agent that initiated the closure.  Negative numbers used for I/O issues.
     */
    void closeAgent(int reason);
}
