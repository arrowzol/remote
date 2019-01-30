package org.devbar.remote.agents;

import org.devbar.remote.keyboard.Keyboard;
import org.devbar.remote.keyboard.KeyboardReader;
import org.devbar.remote.tunnels.MultiplexTunnel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ChatAgent extends BasicAgent implements KeyboardReader {

    private int keyId;
    private boolean hasFocus;

    @Override
    public void go(MultiplexTunnel multiplexTunnel, boolean isServer, boolean first) {
        keyId = Keyboard.kbd.register(this, "Chat");
    }

    @Override
    public void consume(byte[] buffer, int off, int len) {
        System.out.print("(remote chat) ");
        System.out.print(new String(buffer, 0, len, StandardCharsets.UTF_8));
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
    public boolean keyboardInput(String line) {
        if (isClosed) {
            return false;
        }
        if (line.equals("bye")) {
            writer.closeWriter();
        } else {
            byte[] buffer = (line + "\n").getBytes(StandardCharsets.UTF_8);
            try {
                writer.write(buffer, 0, buffer.length);
            } catch (IOException e) {
                writer.closeWriter();
            }
            if (hasFocus) {
                System.out.print(": ");
            }
        }
        return true;
    }

    @Override
    public void closeAgent() {
        super.closeAgent();
        Keyboard.kbd.unregister(keyId);
    }
}
