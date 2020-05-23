package org.devbar.remote.agents;

import org.devbar.remote.keyboard.Keyboard;
import org.devbar.remote.keyboard.KeyboardReader;
import org.devbar.remote.listeners.SocketListener;
import org.devbar.remote.tunnels.MultiplexTunnel;
import org.devbar.remote.utils.Bytes;

import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

public class CommandAgent implements Agent, KeyboardReader {

    private boolean isServer;
    private int kbdId;
    private MultiplexTunnel multiplexTunnel;
    private Writer writer;
    private SortedMap<String, SocketListener> listeners = new TreeMap<>();

    @Override
    public void init(MultiplexTunnel multiplexTunnel, Writer writer, boolean isServer, boolean first) {
        this.multiplexTunnel = multiplexTunnel;
        this.writer = writer;
        this.isServer = isServer;
        if (isServer) {
            kbdId = Keyboard.kbd.register(this, "Command");
        }
    }

    @Override
    public void consume(Bytes bytes) {
        if (!isServer) {
            System.out.print(bytes.toStr());
        }
    }

    @Override
    public void startKeyboardFocus() {
        System.out.print("% ");
    }

    @Override
    public synchronized boolean keyboardInput(String line) {
        if (writer == null) {
            return false;
        }
        try {
            Scanner scanner = new Scanner(line);
            if (scanner.hasNext()) {
                String cmd = scanner.next();
                switch (cmd) {
                    case "ps":
                        for (String name : listeners.keySet()) {
                            System.out.println(name);
                        }
                        break;
                    case "kill":
                        SocketListener listener = listeners.get(scanner.next());
                        listener.close();
                        break;
                    case "fwd":
                        String p1 = scanner.next();
                        int listenerPort;
                        int remotePort;
                        if (p1.equals("vnc")) {
                            listenerPort = 5900;
                            remotePort = 5900;
                        } else {
                            listenerPort = Integer.parseInt(p1);
                            remotePort = Integer.parseInt(scanner.next());
                        }
                        listeners.put(
                                listenerPort + " to " + remotePort,
                                new SocketListener(multiplexTunnel, listenerPort, remotePort));
                        break;
                    case "help":
                    case "?":
                        System.out.println("command agent:");
                        System.out.println("  ps - show active commands");
                        System.out.println("  kill <id> - stop a command");
                        System.out.println("  fwd vnc - port forward for vnc");
                        System.out.println("  fwd <listen-port> <remote-port> - start port forwarding");
                        break;
                    case "cp":
                        multiplexTunnel.registerAgent(new CpAgent(scanner.next(), scanner.next(), false));
                        break;
                    default:
                        System.out.println("unknown command");
                        break;
                }
            }
            System.out.print("% ");
        } catch (Exception e) {
            System.out.println("That didn't work");
        }
        return true;
    }

    @Override
    public synchronized void closeAgent(int reason) {
        if (isServer) {
            Keyboard.kbd.unregister(kbdId);
        }
    }
}
