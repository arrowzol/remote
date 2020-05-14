package org.devbar.remote.tunnels;

import org.devbar.remote.agents.Agent;
import org.devbar.remote.agents.AgentFactory;
import org.devbar.remote.agents.Writer;
import org.devbar.remote.utils.Bytes;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * When an agent closes.
 */
public class MultiplexTunnel implements Tunnel {
    private static final int CMDID_BYTES = 1;
    private static final int CHANNEL_BYTES = 2;
    private static final int LENGTH_BYTES = 2;
    private static final int HEADER_BYTES = CHANNEL_BYTES + LENGTH_BYTES;
    private static final int CMD_PARAM_BYTES = 2;
    private static final int CMD_BYTES = CMDID_BYTES + CHANNEL_BYTES + CMD_PARAM_BYTES;
    private static final int MAX_CHANNELS = 1 << (8* CHANNEL_BYTES);

    private static final byte CMD_AGENT_KILL = 0;
    private static final byte CMD_AGENT_NEW = 2;
    private static final byte CMD_AGENT_NEW_ACK1 = 3;
    private static final byte CMD_AGENT_NEW_ACK2 = 4;

    private Writer parentWriter;
    private boolean isServer;

    private int startChannel;
    private int endChannel;
    private int nextChannel;
    private final ConcurrentMap<Integer, Agent> agents;

    private Agent consumerAgent;
    private boolean waitingForHeader = true;
    private int consumerNeeded;


    public MultiplexTunnel() {
        agents = new ConcurrentHashMap<>();

        // Create the "root" agent, which is the only agent registered with the SocketTunnel.
        // It is the multiplexer.
        agents.put(0, new Agent() {

            @Override
            public void consume(Bytes bytes) {
                int cmd = bytes.readInt(CMDID_BYTES);
                int channel = bytes.readInt(CHANNEL_BYTES);
                int cmdParam = bytes.readInt(CMD_PARAM_BYTES);
                switch (cmd) {
                    case CMD_AGENT_KILL: { // KILL(channel, reason): kill a remote agent
                        Agent agent = agents.remove(channel);
                        if (agent != null) {
                            agent.closeAgent(cmdParam);
                        }
                        break;
                    }
                    case CMD_AGENT_NEW: { // NEW(channel, agentId): create but don't start a remote agent, send ACK1
                        Agent agent = AgentFactory.agentFactory.createAgent(cmdParam);
                        if (agent != null) {
                            agents.put(channel, agent);
                            sendCmd(CMD_AGENT_NEW_ACK1, channel, 0); // success
                        } else {
                            sendCmd(CMD_AGENT_NEW_ACK1, channel, 1); // fail

                        }
                        break;
                    }
                    case CMD_AGENT_NEW_ACK1: { // ACK1(channel, success/fail): start local agent, send ACK2
                        if (cmdParam == 0) {
                            Agent agent = agents.get(channel);
                            sendCmd(CMD_AGENT_NEW_ACK2, channel, 0);
                            agent.init(MultiplexTunnel.this, createWriter(channel), isServer, true);
                        } else {
                            agents.remove(channel);
                            System.out.println("REMOTE AGENT CREAE FAILED");
                        }
                        break;
                    }
                    case CMD_AGENT_NEW_ACK2: { // ACK2(channel): start a remote agent
                        Agent agent = agents.get(channel);
                        agent.init(MultiplexTunnel.this, createWriter(channel), isServer, false);
                        break;
                    }
                }
            }

            @Override
            public void closeAgent(int reason) {
            }

            @Override
            public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
            }
        });
    }


    /* Format: [ header[ channel=0 | length=CMD_BYTES ] | cmd | channel | param ]
     */
    private void sendCmd(byte cmd, int channel, int cmdParam) {
        byte[] command = new byte[HEADER_BYTES + CMD_BYTES];

        // Header of message: channel 0 (by default), length
        Bytes.writeInt(command, CHANNEL_BYTES, LENGTH_BYTES, CMD_BYTES);

        // Body of message: command + channel + cmdParam
        command[HEADER_BYTES] = cmd;
        Bytes.writeInt(command, HEADER_BYTES + 1, CHANNEL_BYTES, channel);
        Bytes.writeInt(command, HEADER_BYTES + 1 + CHANNEL_BYTES, CMD_PARAM_BYTES, cmdParam);

        synchronized (parentWriter) {
            try {
                parentWriter.write(command, 0, command.length);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public synchronized void registerAgent(Agent agent) {
        int channel;
        do {
            channel = nextChannel++;
            if (nextChannel >= endChannel) {
                nextChannel = startChannel;
            }
        } while (agents.containsKey(channel));

        agents.put(channel, agent);
        sendCmd(CMD_AGENT_NEW, channel, AgentFactory.agentFactory.agentId(agent));
    }

    private Writer createWriter(int channel) {
        byte[] header = new byte[HEADER_BYTES];
        Bytes.writeInt(header, 0, CHANNEL_BYTES, channel);

        return new Writer() {
            private boolean open = true;

            @Override
            public synchronized boolean write(byte[] buffer, int start, int len) throws IOException {
                if (open) {
                    Bytes.writeInt(header, CHANNEL_BYTES, LENGTH_BYTES, len);
                    synchronized (parentWriter) {
                        parentWriter.write(header, 0, HEADER_BYTES);
                        parentWriter.write(buffer, start, len);
                    }
                }
                return open;
            }

            @Override
            public synchronized void closeWriter(int reason) {
                open = false;
                Agent agent = agents.remove(channel);
                if (agent != null) {
                    agent.closeAgent(reason);
                }
                sendCmd(CMD_AGENT_KILL, channel, reason);
            }
        };
    }

    @Override
    public void consume(Bytes bytes) {
        while (true) {
            int size = bytes.size();
            if (waitingForHeader) {
                if (size < HEADER_BYTES) {
                    return;
                }
                int channel = bytes.readInt(CHANNEL_BYTES);
                consumerNeeded = bytes.readInt(LENGTH_BYTES);
                consumerAgent = agents.get(channel);
                waitingForHeader = false;
                size = bytes.size();
            }

            // If there is no agent, ignore consumerNeeded bytes
            if (consumerAgent == null) {
                consumerNeeded -= bytes.skip(consumerNeeded);
            }

            // If one complete message is in the copy, send it to the agent
            else if (size >= consumerNeeded || bytes.full()) {
                long snapshot = bytes.takeSnapshot();
                int actualSize = bytes.setSize(consumerNeeded);
                consumerNeeded -= actualSize;
                try {
                    consumerAgent.consume(bytes);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                bytes.restoreSnapshot(snapshot);
                bytes.skip(actualSize);
            }

            if (consumerNeeded == 0) {
                waitingForHeader = true;
            } else {
                break;
            }
        }
    }

    @Override
    public void closeAgent(int reason) {
        for (Agent agent : agents.values()) {
            agent.closeAgent(reason);
        }
    }

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        this.isServer = isServer;
        if (isServer) {
            startChannel = 1;
            endChannel = MAX_CHANNELS/2;
        } else {
            startChannel = MAX_CHANNELS/2;
            endChannel = MAX_CHANNELS;
        }
        nextChannel = startChannel;
        this.parentWriter = writer;
    }

    @Override
    public void closeWriter(int reason) {
        throw new RuntimeException("Should never happen");
    }

    @Override
    public boolean write(byte[] buffer, int start, int len) {
        throw new RuntimeException("Should never happen");
    }
}
