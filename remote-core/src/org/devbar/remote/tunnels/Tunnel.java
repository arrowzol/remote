package org.devbar.remote.tunnels;

import org.devbar.remote.agents.Agent;
import org.devbar.remote.agents.Writer;

/** A tunnel represents a channel of communication for agents.  It is an agent (it will consume communication) and a
 * writer (must be able to send communication).  Agents can register with a tunnel.  Some tunnels only allow one
 * agent, others allow many.
 */
public interface Tunnel extends Agent, Writer {
    void registerAgent(Agent agent);
}
