package org.devbar.remote.tunnels;

import org.devbar.remote.agents.Agent;
import org.devbar.remote.agents.Writer;

public interface Tunnel extends Agent, Writer {
    void registerAgent(Agent agent);
}
