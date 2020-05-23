package org.devbar.remote.keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Keyboard extends Thread {

    public static Keyboard kbd = new Keyboard();

    private final static int CMD_ID = -1;
    private int nextId = 1;
    private SortedMap<Integer, KeyEntry> keyQueues = new TreeMap<>();
    private KeyEntry currentFocus;

    private Keyboard() {
        super("keyboardInput reader");
        setDaemon(true);
        start();
    }

    private static class KeyEntry {
        String name;
        KeyboardReader keyboardReader;

        KeyEntry(KeyboardReader keyboardReader, String name) {
            this.keyboardReader = keyboardReader;
            this.name = name;
        }
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (true) {
                if (currentFocus == null) {
                    System.out.print("# ");
                }
                String str = reader.readLine().trim();

                if (str.equals("z")) {
                    if (currentFocus != null) {
                        currentFocus.keyboardReader.endKeyboardFocus();
                    }
                    currentFocus = null;
                } else {
                    if(str.equals("?") || str.equals("help")) {
                        System.out.println("Keyboard:");
                        System.out.println("  z - suspend current agent");
                        System.out.println("  ps - show active keyboard agents");
                        System.out.println("  use <id> - use specified keyboard agent");
                    }
                    if (currentFocus == null) {
                        if ("ps".equals(str)) {
                            for (Map.Entry<Integer, KeyEntry> entry : keyQueues.entrySet()) {
                                System.out.println(entry.getKey() + " : " + entry.getValue().name);
                            }
                        } else if (str.startsWith("use ")) {
                            try {
                                int useKeyId = Integer.parseInt(str.substring(4));
                                if (currentFocus != null) {
                                    currentFocus.keyboardReader.endKeyboardFocus();
                                }
                                currentFocus = keyQueues.get(useKeyId);
                                if (currentFocus != null) {
                                    currentFocus.keyboardReader.startKeyboardFocus();
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                        } else {
                            // send cmd to CommandAgent
                        }
                    } else if (currentFocus != null) {
                        if (!currentFocus.keyboardReader.keyboardInput(str)) {
                            System.out.println("LOST: " + currentFocus.name);
                            currentFocus = null;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int register(KeyboardReader keyboardReader, String name) {
        int keyId = nextId++;
        KeyEntry newKeyEntry = new KeyEntry(keyboardReader, name);
        keyQueues.put(keyId, newKeyEntry);
        if (currentFocus == null) {
            currentFocus = newKeyEntry;
            currentFocus.keyboardReader.startKeyboardFocus();
        }
        return keyId;
    }

    public void unregister(int keyId) {
        keyQueues.remove(keyId);
    }
}
