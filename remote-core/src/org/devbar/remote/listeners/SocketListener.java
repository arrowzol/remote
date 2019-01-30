package org.devbar.remote.listeners;

import org.devbar.remote.agents.PortForwardAgent;
import org.devbar.remote.tunnels.MultiplexTunnel;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketListener extends Thread {
    public MultiplexTunnel multiplexTunnel;
    int listenPort;
    int remotePort;
    private ServerSocket ss;

    public SocketListener(MultiplexTunnel multiplexTunnel, int listenPort, int remotePort) throws IOException {
        this.multiplexTunnel = multiplexTunnel;
        this.listenPort = listenPort;
        this.remotePort = remotePort;
        ss = ServerSocketFactory.getDefault().createServerSocket(listenPort);
        setName("port-forward-listener-" + listenPort + "-" + remotePort);
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = ss.accept();
                multiplexTunnel.registerAgent(new PortForwardAgent(socket, remotePort));
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public void close() {
        try {
            ss.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
