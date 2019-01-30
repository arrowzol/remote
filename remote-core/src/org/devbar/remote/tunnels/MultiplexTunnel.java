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
    private static final int CHANNEL_BYTES = 2;
    private static final int LENGTH_BYTES = 2;
    private static final int HEADER_BYTES = CHANNEL_BYTES + LENGTH_BYTES;
    private static final int AGENT_BYTES = 2;
    private static final int CMD_BYTES = 1 + CHANNEL_BYTES + AGENT_BYTES;
    private static final int MAX_CHANNELS = 1 << (8* CHANNEL_BYTES);

    private static final byte CMD_AGENT_KILL = 0;
    private static final byte CMD_AGENT_DEAD = 1;
    private static final byte CMD_AGENT_NEW = 2;
    private static final byte CMD_AGENT_NEW_ACK1 = 3;
    private static final byte CMD_AGENT_NEW_ACK2 = 4;

    private Writer masterWriter;

    private final int startChannel;
    private final int endChannel;
    private int nextChannel;
    private final ConcurrentMap<Integer, Agent> agents;

    private Agent consumerAgent;
    private int consumerNeeded;
    private int headerNeeded = HEADER_BYTES;
    private final byte[] consumeHeader = new byte[HEADER_BYTES + CMD_BYTES];
    private int consumerBufferHead;
    private byte[] consumerBuffer;


    public MultiplexTunnel(boolean isServer, int bufferSize) {
        if (isServer) {
            startChannel = 1;
            endChannel = MAX_CHANNELS/2;
        } else {
            startChannel = MAX_CHANNELS/2;
            endChannel = MAX_CHANNELS;
        }
        nextChannel = startChannel;

        if (bufferSize < CMD_BYTES) {
            bufferSize = CMD_BYTES;
        }
        consumerBuffer = new byte[bufferSize];

        agents = new ConcurrentHashMap<>();
        agents.put(0, new Agent() {
            @Override
            public void registerWriter(Writer writer) {
            }

            @Override
            public void consume(byte[] buffer, int off, int len) {
                int channel = Bytes.read(buffer, off + 1, CHANNEL_BYTES);
                int agentId = Bytes.read(buffer, off + 1 + CHANNEL_BYTES, AGENT_BYTES);
                switch (buffer[off]) {
                    case CMD_AGENT_KILL: {
                        Agent agent = agents.get(channel);
                        if (agent != null) {
                            agent.closeAgent();
                        }
                        sendCmd(CMD_AGENT_KILL, channel, 0);
                        break;
                    }
                    case CMD_AGENT_DEAD:
                        agents.remove(channel);
                        break;
                    case CMD_AGENT_NEW: {
                        Agent agent = AgentFactory.agentFactory.createAgent(agentId);
                        if (agent != null) {
                            agents.put(channel, agent);
                            attachWriterToAgent(agent, channel);
                            sendCmd(CMD_AGENT_NEW_ACK1, channel, 0);
                        }
                        break;
                    }
                    case CMD_AGENT_NEW_ACK1: {
                        Agent agent = agents.get(channel);
                        sendCmd(CMD_AGENT_NEW_ACK2, channel, 0);
                        agent.go(MultiplexTunnel.this, isServer, true);
                        break;
                    }
                    case CMD_AGENT_NEW_ACK2: {
                        Agent agent = agents.get(channel);
                        agent.go(MultiplexTunnel.this, isServer, false);
                        break;
                    }
                }
            }

            @Override
            public void closeAgent() {
            }
        });
    }


    /* Format: channel(CHANNEL_BYTES) - length - command(1)
     */
    private void sendCmd(byte cmd, int channel, int agentId) {
        byte[] command = new byte[HEADER_BYTES + CMD_BYTES];

        // Header of message: channel 0 (by default), length
        Bytes.write(command, CHANNEL_BYTES, LENGTH_BYTES, CMD_BYTES);

        // Body of message: command + channel + agentId
        command[HEADER_BYTES] = cmd;
        Bytes.write(command, HEADER_BYTES + 1, CHANNEL_BYTES, channel);
        Bytes.write(command, HEADER_BYTES + 1 + CHANNEL_BYTES, AGENT_BYTES, agentId);

        synchronized (masterWriter) {
            try {
                masterWriter.write(command, 0, command.length);
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
        attachWriterToAgent(agent, channel);
        sendCmd(CMD_AGENT_NEW, channel, AgentFactory.agentFactory.agentId(agent));
    }

    private void attachWriterToAgent(Agent agent, int channel) {
        byte[] header = new byte[HEADER_BYTES];
        Bytes.write(header, 0, CHANNEL_BYTES, channel);

        agent.registerWriter(new Writer() {
            private boolean open = true;

            @Override
            public void write(byte[] buffer, int start, int len) throws IOException {
                if (open) {
                    Bytes.write(header, CHANNEL_BYTES, LENGTH_BYTES, len);
                    synchronized (masterWriter) {
                        masterWriter.write(header, 0, HEADER_BYTES);
                        masterWriter.write(buffer, start, len);
                    }
                }
            }

            @Override
            public void closeWriter() {
                if (open) {
                    open = false;
                    sendCmd(CMD_AGENT_KILL, channel, 0);
                    sendCmd(CMD_AGENT_DEAD, channel, 0);
                }
            }
        });

    }

    @Override
    public void consume(byte[] buffer, int off, int len) {
        while (true) {
            if (headerNeeded > 0) {
                int copyLen = headerNeeded;
                if (len < copyLen) {
                    copyLen = len;
                }
                System.arraycopy(buffer, off, consumeHeader, HEADER_BYTES - headerNeeded, copyLen);
                off += copyLen;
                len -= copyLen;
                headerNeeded -= copyLen;

                if (headerNeeded == 0) {
                    int channel = Bytes.read(consumeHeader, 0, CHANNEL_BYTES);
                    consumerNeeded = Bytes.read(consumeHeader, CHANNEL_BYTES, LENGTH_BYTES);
                    consumerAgent = agents.get(channel);
                }
            }

            if (len == 0) {
                break;
            }

            if (consumerNeeded > 0) {
                // If there is no agent, ignore consumerNeeded bytes
                if (consumerAgent == null) {
                    off += consumerNeeded;
                    len -= consumerNeeded;
                    consumerNeeded = 0;
                }

                // If one complete message is in the buffer, send it to the agent
                else if (consumerBufferHead == 0 && len >= consumerNeeded) {
                    try {
                        consumerAgent.consume(buffer, off, consumerNeeded);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    off += consumerNeeded;
                    len -= consumerNeeded;
                    consumerNeeded = 0;
                }

                // Copy into the consumerBuffer
                else {
                    int copyLen = consumerBuffer.length - consumerBufferHead;
                    if (copyLen > len) {
                        copyLen = len;
                    }
                    if (copyLen > consumerNeeded) {
                        copyLen = consumerNeeded;
                    }
                    System.arraycopy(buffer, off, consumerBuffer, consumerBufferHead, copyLen);
                    consumerBufferHead += copyLen;
                    off += copyLen;
                    len -= copyLen;
                    consumerNeeded -= copyLen;

                    // If consumerBuffer is full, or there is a complete message, send it to the agent
                    if (consumerBufferHead == consumerBuffer.length || consumerNeeded == 0) {
                        try {
                            consumerAgent.consume(consumerBuffer, 0, consumerBufferHead);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        consumerBufferHead = 0;
                    }
                }

                if (consumerNeeded == 0) {
                    headerNeeded = HEADER_BYTES;
                }
            }

            if (len == 0) {
                break;
            }
        }
    }

    @Override
    public void registerWriter(Writer writer) {
        masterWriter = writer;
    }

    @Override
    public void closeAgent() {
        for (Agent agent : agents.values()) {
            agent.closeAgent();
        }
    }

    @Override
    public void closeWriter() {
        masterWriter.closeWriter();
    }

    @Override
    public void write(byte[] buffer, int start, int len) {
        throw new RuntimeException("Should never happen");
    }
}
