package org.devbar.remote.tunnels;

import org.devbar.remote.agents.Agent;
import org.devbar.remote.agents.Writer;
import org.devbar.remote.utils.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketTunnel extends Thread implements Tunnel {
    public static final int MAX_FAST_MESSAGE_SIZE = 1024*32;
    private static int i;
    private final Socket s;
    private final InputStream is;
    private final OutputStream os;
    private Agent agent;
    private boolean isServer;
    private boolean agentClosed;

    public SocketTunnel(Socket s, boolean isServer) throws IOException {
        this.isServer = isServer;
        this.s = s;
        is = s.getInputStream();
        os = s.getOutputStream();
    }

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        throw new RuntimeException("Should never be called");
    }

    @Override
    public void run() {
        try {
            Bytes buffer = new Bytes(MAX_FAST_MESSAGE_SIZE);
            while (true) {
                if (!buffer.read(is)) {
                    break;
                }
                try {
                    agent.consume(buffer);
                    buffer.clear();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (agent) {
                if (!agentClosed) {
                    agent.closeAgent(-1);
                    agentClosed = true;
                }
            }
        }
    }

    @Override
    public void registerAgent(Agent agent) {
        if (this.agent != null) {
            throw new RuntimeException("SocketTunnel can have only one agent");
        }

        this.agent = agent;
        agent.init(null, this, isServer, isServer);

        this.setName("Socket Reader " + i++);
        this.start();
    }

    @Override
    public void consume(Bytes bytes) {
        throw new RuntimeException("Should never be called");
    }

    @Override
    public synchronized boolean write(byte[] buffer, int off, int len) throws IOException {
        os.write(buffer, off, len);
        return true;
    }

    @Override
    public void closeAgent(int reason) {
        throw new RuntimeException("Should never be called");
        // closeWriter(reason);
    }

    @Override
    public void closeWriter(int reason) {
        boolean alreadyClosed;
        synchronized (agent) {
            alreadyClosed = agentClosed;
            agentClosed = true;
        }
        System.out.println("Socket Closed");
        // TODO: transmit reason to remote system
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!alreadyClosed) {
            agent.closeAgent(reason);
        }
    }
}
