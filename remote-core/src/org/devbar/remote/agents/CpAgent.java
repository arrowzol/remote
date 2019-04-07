package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import java.io.*;

import static org.devbar.remote.tunnels.SocketTunnel.MAX_FAST_MESSAGE_SIZE;

public class CpAgent extends Thread implements Agent {

    public static final int MAX_TX_BUFFER_SIZE = 1024 * 8;

    private enum Stage {ZERO, INIT, SEND, RECEIVE}

    private String from;
    private String to;
    private boolean reverse;

    private Stage stage = Stage.ZERO;
    private boolean first;
    private boolean isSender;

    private InputStream fileInputStream;
    private OutputStream fileOutputStream;
    private Writer writer;

    public CpAgent() {
    }

    public CpAgent(String from, String to, boolean reverse) {
        this.from = from;
        this.to = to;
        this.reverse = reverse;
    }

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        this.writer = writer;
        this.first = first;
        if (first) {
            try {
                Bytes.writeStr(writer, from);
                Bytes.writeStr(writer, to);
                Bytes.writeBool(writer, reverse);
            } catch (IOException e) {
                e.printStackTrace();
            }
            init();
        } else {
            stage = Stage.INIT;
        }
    }

    private void init() {
        isSender = first ^ reverse;
        if (isSender) {
            stage = Stage.SEND;
            try {
                fileInputStream = new BufferedInputStream(new FileInputStream(from));
            } catch (FileNotFoundException e) {
                closeAgent();
                return;
            }
            start();
        } else {
            stage = Stage.RECEIVE;
            try {
                fileOutputStream = new BufferedOutputStream(new FileOutputStream(from));
            } catch (FileNotFoundException e) {
                closeAgent();
            }
        }
    }

    @Override
    public void consume(Bytes bytes) {
        switch (stage) {
            case INIT:
                from = bytes.readStr();
                if (from == null) {
                    closeAgent();
                }
                to = bytes.readStr();
                if (to == null) {
                    closeAgent();
                }
                Boolean nullableReverse = bytes.readBool();
                if (nullableReverse == null) {
                    closeAgent();
                }
                reverse = nullableReverse;
                init();
                break;
            case RECEIVE:
                try {
                    bytes.write(fileOutputStream);
                } catch (IOException e) {
                    this.closeAgent();
                }
                break;
            case SEND:
                // TODO: report on success
        }
    }

    @Override
    public void run() {
        int bufferSize = MAX_FAST_MESSAGE_SIZE/2;
        if (bufferSize > MAX_TX_BUFFER_SIZE) {
            bufferSize = MAX_TX_BUFFER_SIZE;
        }
        byte[] bytes = new byte[bufferSize];
        try {
            while (true) {
                int len = fileInputStream.read(bytes, 0, bytes.length);
                if (len == 0) {
                    return;
                }
                writer.write(bytes, 0, len);
            }
        } catch (IOException e) {
            closeAgent();
        }
        // TODO: ??
    }

    @Override
    public synchronized void closeAgent() {
        if (writer != null) {
            this.interrupt();
            writer.closeWriter();
            writer = null;
            try {
                fileOutputStream.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                fileInputStream.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
