package org.devbar.remote.agents;

import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import java.io.*;

import static org.devbar.remote.tunnels.SocketTunnel.MAX_FAST_MESSAGE_SIZE;

public class CpAgent extends Thread implements Agent {

    private static final int REASON_DONE = 1;
    private static final int REASON_ERROR = 2;
    private static final int REASON_DUPLICATE = 3;
    private static final int REASON_MISSING = 4;
    private static final int REASON_EXISTS = 5;
    private static final int REASON_FILE_IO = 6;

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
                writer.closeWriter(REASON_ERROR);
                e.printStackTrace();
            }
            init();
        } else {
            stage = Stage.INIT;
        }
    }

    private void init() {
        // first is the sender (unless reversed)
        isSender = first != reverse;
        if (isSender) {
            stage = Stage.SEND;
            try {
                fileInputStream = new BufferedInputStream(new FileInputStream(from));
            } catch (FileNotFoundException e) {
                writer.closeWriter(REASON_MISSING);
                return;
            }
            start();
        } else {
            stage = Stage.RECEIVE;
            File toFile = new File(to);
            if (toFile.exists()) {
                writer.closeWriter(REASON_EXISTS);
            } else {
                try {
                    fileOutputStream = new BufferedOutputStream(new FileOutputStream(toFile));
                } catch (FileNotFoundException e) {
                    writer.closeWriter(REASON_FILE_IO);
                }
            }
        }
    }

    @Override
    public void consume(Bytes bytes) {
        switch (stage) {
            case INIT:
                from = bytes.readStr();
                if (from == null) {
                    writer.closeWriter(REASON_ERROR);
                }
                to = bytes.readStr();
                if (to == null) {
                    writer.closeWriter(REASON_ERROR);
                }
                Boolean nullableReverse = bytes.readBool();
                if (nullableReverse == null) {
                    writer.closeWriter(REASON_ERROR);
                } else {
                    reverse = nullableReverse;
                    init();
                }
                break;
            case RECEIVE:
                try {
                    bytes.write(fileOutputStream);
                } catch (IOException e) {
                    writer.closeWriter(REASON_FILE_IO);
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
                    writer.closeWriter(REASON_DONE);
                    break;
                }
                if (!writer.write(bytes, 0, len)) {
                    break;
                }
            }
        } catch (IOException e) {
            writer.closeWriter(REASON_FILE_IO);
        }
    }

    @Override
    public synchronized void closeAgent(int reason) {
        this.interrupt();
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

        if (reason != REASON_DONE) {
            // delete the file
        }
    }
}
