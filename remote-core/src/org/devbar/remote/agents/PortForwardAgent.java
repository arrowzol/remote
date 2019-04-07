package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PortForwardAgent extends Thread implements Agent {

    public static final int PORT_BYTES = 4;
    private Writer writer;

    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private int remotePort;
    private boolean started;

    public PortForwardAgent() {
    }

    public PortForwardAgent(Socket socket, int remotePort) throws IOException {
        this.socket = socket;
        is = socket.getInputStream();
        os = socket.getOutputStream();
        started = true;
        this.remotePort = remotePort;
    }

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        this.writer = writer;
        if (first) {
            byte[] portAsBytes = new byte[4];
            Bytes.writeInt(portAsBytes, 0, PORT_BYTES, remotePort);
            try {
                writer.write(portAsBytes);
            } catch (IOException e) {
                closeAgent();
            }
            setName("port-forward-agent");
            setDaemon(true);
            start();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024*8];
        try {
            while (true) {
                int len = is.read(buffer, 0, buffer.length);
                if (len == -1) {
                    break;
                }
                writer.write(buffer, 0, len);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            closeAgent();
        }
    }

    @Override
    public void consume(byte[] buffer, int off, int len) {
        try {
            if (started) {
                    os.write(buffer, off, len);
            } else {
                started = true;
                Bytes bytes = new Bytes(buffer, off, PORT_BYTES);
                remotePort = bytes.readInt(len);
                SocketFactory sf = SocketFactory.getDefault();
                socket = sf.createSocket("localhost", remotePort);
                is = socket.getInputStream();
                os = socket.getOutputStream();
                setName("port-forward-agent");
                setDaemon(true);
                start();
            }
        } catch (IOException e) {
            closeAgent();
        }
    }

    @Override
    public synchronized void closeAgent() {
        if (writer != null) {
            writer.closeWriter();
            writer = null;
            this.interrupt();
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
