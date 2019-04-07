package org.devbar.remote.agents;

import org.devbar.remote.keyboard.Keyboard;
import org.devbar.remote.keyboard.KeyboardReader;
import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ChatAgent implements Agent, KeyboardReader {

    private int keyId;
    private boolean hasFocus;
    private Writer writer;

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        this.writer = writer;
        keyId = Keyboard.kbd.register(this, "Chat");
    }

    @Override
    public void consume(Bytes bytes) {
        System.out.print("(remote chat) ");
        System.out.print(bytes.toStr());
        if (hasFocus) {
            System.out.print(": ");
        }
    }

    @Override
    public void startKeyboardFocus()
    {
        System.out.println("Chat!");
        System.out.print(": ");
        hasFocus = true;
    }

    @Override
    public void endKeyboardFocus() {
        System.out.println("Chat out");
        hasFocus = false;
    }

    @Override
    public synchronized boolean keyboardInput(String line) {
        if (writer == null) {
            return false;
        }
        if (line.equals("bye")) {
            writer.closeWriter();
        } else {
            byte[] buffer = (line + "\n").getBytes(StandardCharsets.UTF_8);
            try {
                writer.write(buffer, 0, buffer.length);
            } catch (IOException e) {
                closeAgent();
            }
            if (hasFocus) {
                System.out.print(": ");
            }
        }
        return true;
    }

    @Override
    public synchronized void closeAgent() {
        if (writer != null) {
            writer.closeWriter();
            writer = null;
            Keyboard.kbd.unregister(keyId);
        }
    }
}
