package org.devbar.remote.agents;

import java.util.HashMap;
import java.util.Map;

public class AgentFactory {
    public static AgentFactory agentFactory = new AgentFactory();

    private final Map<Integer, Class> prototypes = new HashMap();
    private final Map<Class, Integer> agentIds = new HashMap();
    private int i;

    public AgentFactory() {
        register(ChatAgent.class);
        register(CommandAgent.class);
        register(PortForwardAgent.class);
    }

    public Agent createAgent(int id) {
        Class prototype = prototypes.get(id);
        if (prototype == null) {
            return null;
        }
        try {
            return (Agent) prototype.getConstructor(null).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void register(Class agent) {
        int id = i++;
        prototypes.put(id, agent);
        agentIds.put(agent, id);
    }

    public int agentId(Agent agent) {
        Class clazz = agent.getClass();
        return agentIds.get(clazz);
    }
}
