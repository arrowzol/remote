package org.devbar.remote.client;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.tunnels.SocketTunnel;
import org.devbar.remote.tunnels.Tunnel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

public class Client {

    public static void main(String[] args) throws Exception {
        SSLContext context = getSslContext();
        context.getClientSessionContext();
        SSLSocketFactory factory = context.getSocketFactory();

        SSLSocket socket =(SSLSocket)factory.createSocket(Constants.HOST, Constants.PORT);
        socket.startHandshake();

        Tunnel tunnel = new SocketTunnel(socket, false);
        tunnel.registerAgent(new MultiplexTunnel(0));
    }

    private static SSLContext getSslContext() throws Exception {
        // set up key manager to do server authentication
        char[] passphrase = "changeit".toCharArray();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore tmks = KeyStore.getInstance("JKS");
        try (InputStream trustInput = Client.class.getClassLoader().getResourceAsStream("trusts.jks")) {
            if (trustInput == null) {
                throw new RuntimeException("can't read trusts.jks");
            }
            tmks.load(trustInput, passphrase);
        }
        tmf.init(tmks);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);

        return ctx;
    }

}
