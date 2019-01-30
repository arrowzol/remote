package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CpAgent extends BasicAgent {

    private String from;
    private String to;
    private boolean reverse;

    private boolean started;
    private Bytes bytes;

    private InputStream is;
    private OutputStream os;

    public CpAgent() {
    }

    public CpAgent(String from, String to, boolean reverse) {
        this.from = from;
        this.to = to;
        this.reverse = reverse;
    }

    @Override
    public void consume(byte[] buffer, int off, int len) {
        if (started) {
            try {
                os.write(buffer, off, len);
            } catch (IOException e) {
                this.closeAgent();
            }
        } else {
            bytes.buffer(buffer, off, len);
            if (from == null) {
                from = bytes.readStr();
            }
            if (to == null) {
                to = bytes.readStr();
            }
            if (to != null) {
                started = true;
                // TODO
            }
        }
    }

    @Override
    public void go(MultiplexTunnel multiplexTunnel, boolean isServer, boolean first) {
        if (first) {
            try {
                Bytes.writeStr(writer, from);
                Bytes.writeStr(writer, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            start();
        } else {
            bytes = new Bytes(1024);
        }
    }

    @Override
    public synchronized void closeAgent() {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
            os = null;
        }
    }
}
