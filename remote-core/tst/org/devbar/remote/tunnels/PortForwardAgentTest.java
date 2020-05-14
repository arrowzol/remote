package org.devbar.remote.tunnels;

import org.devbar.remote.listeners.SocketListener;
import org.junit.jupiter.api.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortForwardAgentTest extends MockMultiplexSystem {

    @Test
    public void test() throws Exception {
        init();

        Thread xfer = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        quiescent(1000);
                        Thread.sleep(100);
                    }
                } catch (Exception e) {

                }
            }
        };

        xfer.setName("xfer-agent-junit");
        xfer.setDaemon(true);
        xfer.start();
        ServerSocket ss = null;
        SocketListener sl = null;
        Socket s1 = null;
        Socket s2 = null;

        try {
            sl = new SocketListener(testTunnelA, 8081, 8082);

            ss = ServerSocketFactory.getDefault().createServerSocket(8082);
            s1 = SocketFactory.getDefault().createSocket("localhost", 8081);
            s2 = ss.accept();

            InputStream is1 = s1.getInputStream();
            OutputStream os1 = s1.getOutputStream();

            InputStream is2 = s2.getInputStream();
            OutputStream os2 = s2.getOutputStream();

            String testStringA = "12345";
            byte[] testBytesA = testStringA.getBytes();
            String testStringB = "abc";
            byte[] testBytesB = testStringB.getBytes();

            os1.write(testBytesA);
            os2.write(testBytesB);

            byte[] buffer = new byte[1024];

            int len1 = is1.read(buffer, 0, 1024);
            int len2 = is2.read(buffer, len1, 1024-len1);

            assertEquals(testStringB, new String(buffer, 0, len1));
            assertEquals(testStringA, new String(buffer, len1, len2));

            s1.close();

            int value = is2.read();
            assertTrue(value == -1);
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {

                }
            }
            if (s1 != null) {
                try {
                    s1.close();
                } catch (IOException e) {

                }
            }
            if (s2 != null) {
                try {
                    s2.close();
                } catch (IOException e) {

                }
            }
            sl.close();
            testTunnelA.closeAgent(0);
            testTunnelB.closeAgent(0);

            xfer.interrupt();
        }
    }
}
