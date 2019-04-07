package org.devbar.remote.tunnels;

import org.devbar.remote.agents.Agent;
import org.devbar.remote.agents.Writer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MockAgentBase implements Agent {
    public Writer writer;

    public int closeAgentCount;
    public int goCountFirst;
    public int goCountSecond;
    public int head;
    public byte[] contents = new byte[1024];

    public void write(String testMessage) throws IOException {
        byte[] testBytes = testMessage.getBytes(StandardCharsets.UTF_8);
        writer.write(testBytes, 0, testBytes.length);
    }

    public String read() {
        String value = new String(contents, 0, head, StandardCharsets.UTF_8);
        head = 0;
        return value;
    }

    @Override
    public void consume(byte[] buffer, int off, int len) {
        System.arraycopy(buffer, off, contents, head, len);
        head += len;
    }

    @Override
    public void closeAgent() {
        closeAgentCount++;
    }

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        this.writer = writer;
        if (first) {
            goCountFirst++;
        } else {
            goCountSecond++;
        }
    }
}
