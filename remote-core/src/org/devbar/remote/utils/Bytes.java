package org.devbar.remote.utils;

import org.devbar.remote.agents.Writer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Bytes {
    private byte[] buff;
    private int head;

    public Bytes(int length) {
        buff = new byte[length];
    }

    public static int read(byte[] buffer, int offset, int len) {
        int value = 0;
        for (int i = 0; i < len; i++) {
            value *= 0x100;
            value += 0xff & buffer[offset + i];
        }
        return value;
    }

    public static void write(byte[] buffer, int offset, int len, int value) {
        for (int i = 1; i <= len; i++) {
            buffer[offset + len - i] = (byte) value;
            value /= 0x100;
        }
    }

    public void buffer(byte[] buffer, int offset, int len) {
        System.arraycopy(buffer, offset, buff, head, len);
        head += len;
    }

    public String readStr() {
        for (int i=0; i < head; i++) {
            if (buff[i] == 0) {
                String str = new String(buff, 0, i, StandardCharsets.UTF_8);
                System.arraycopy(buff, i, buff, 0, head-i);
                head -= i;
                return str;
            }
        }
        return null;
    }

    public static void writeStr(Writer writer, String str) throws IOException {
        writer.write(str.getBytes(StandardCharsets.UTF_8));
    }
}
