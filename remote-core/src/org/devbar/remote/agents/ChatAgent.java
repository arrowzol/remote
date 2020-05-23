package org.devbar.remote.agents;

import org.devbar.remote.keyboard.Keyboard;
import org.devbar.remote.keyboard.KeyboardReader;
import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ChatAgent implements Agent, KeyboardReader {

    private static final int REASON_BYE = 1;
    private static final int REASON_IO = 2;

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
        if (line.equals("bye")) {
            writer.closeWriter(REASON_BYE);
        } if (line.equals("?") || line.equals("help")) {
            System.out.println("Chat Agent:");
            System.out.println("  bye - close this chat session");
            System.out.println("  type anything - it will be shown to the other");
        } else {
            byte[] buffer = (line + "\n").getBytes(StandardCharsets.UTF_8);
            try {
                writer.write(buffer, 0, buffer.length);
            } catch (IOException e) {
                writer.closeWriter(REASON_IO);
            }
            if (hasFocus) {
                System.out.print(": ");
            }
        }
        return true;
    }

    @Override
    public synchronized void closeAgent(int reason) {
        System.out.println("Chat closed: " + (reason == REASON_BYE ? "BYE" : "I/O issue"));
        Keyboard.kbd.unregister(keyId);
    }
}
