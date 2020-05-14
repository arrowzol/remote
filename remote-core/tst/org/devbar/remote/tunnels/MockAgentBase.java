package org.devbar.remote.tunnels;

import org.devbar.remote.agents.Agent;
import org.devbar.remote.agents.Writer;
import org.devbar.remote.utils.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MockAgentBase implements Agent {
    public Writer writer;

    public int closeAgentCount;
    public int goCountFirst;
    public int goCountSecond;
    public Bytes contents = new Bytes(1024);

    public void write(String testMessage) throws IOException {
        byte[] testBytes = testMessage.getBytes(StandardCharsets.UTF_8);
        writer.write(testBytes, 0, testBytes.length);
    }

    public String read() {
        String value = contents.toStr();
        contents.clear();
        return value;
    }

    @Override
    public void consume(Bytes bytes) {
        contents.copy(bytes);
    }

    @Override
    public void closeAgent(int reason) {
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
