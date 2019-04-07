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

    public SocketTunnel(Socket s, boolean isServer) throws IOException {
        this.isServer = isServer;
        this.s = s;
        is = s.getInputStream();
        os = s.getOutputStream();
    }

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        // never called
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
            closeAgent();
        }
    }

    @Override
    public void registerAgent(Agent agent) {
        if (this.agent != null) {
            throw new RuntimeException("SocketTunnel can have only one upTunnel");
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
    public synchronized void write(byte[] buffer, int off, int len) throws IOException {
        os.write(buffer, off, len);
    }

    @Override
    public void closeAgent() {
        closeWriter();
    }

    @Override
    public void closeWriter() {
        System.out.println("Socket Closed");
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
        agent.closeAgent();
    }
}
