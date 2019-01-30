package org.devbar.remote.server;

import org.devbar.remote.agents.ChatAgent;
import org.devbar.remote.agents.CommandAgent;
import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.tunnels.SocketTunnel;
import org.devbar.remote.tunnels.Tunnel;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

public class Server {

    public static void main(String[] argv) throws Exception {
        SSLContext ctx = getSslContext("/Users/devbar/server");
        SSLServerSocketFactory ssf = ctx.getServerSocketFactory();
        SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(Constants.PORT);
//        ss.setNeedClientAuth(true);
        System.out.println("Server Ready");
        while (true) {
            SSLSocket socket = (SSLSocket) ss.accept();
            try {
                SSLParameters sslParams = new SSLParameters();
                socket.setSSLParameters(sslParams);
                socket.startHandshake();
                System.out.println("Accept");
                Tunnel socketTunnel = new SocketTunnel(socket);
                MultiplexTunnel multiplexTunnel = new MultiplexTunnel(true, 0);
                socketTunnel.registerAgent(multiplexTunnel);
                multiplexTunnel.registerAgent(new CommandAgent());
                multiplexTunnel.registerAgent(new ChatAgent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static SSLContext getSslContext(String docroot) throws Exception {
        // set up key manager to do server authentication
        char[] passphrase = "changeit".toCharArray();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore kmks = KeyStore.getInstance("PKCS12");
        try (InputStream keyInput =
                     Thread.currentThread().getContextClassLoader().getResourceAsStream("server-key.pfx")) {
            if (keyInput == null) {
                throw new RuntimeException("Can't read server-key.pfx");
            }
            kmks.load(keyInput, passphrase);
        }
        kmf.init(kmks, passphrase);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        return ctx;
    }
}
