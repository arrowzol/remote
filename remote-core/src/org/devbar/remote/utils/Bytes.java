package org.devbar.remote.utils;

import org.devbar.remote.agents.Writer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Bytes {
    private byte[] buffer;
    private int offset;
    private int head;

    public Bytes(int length) {
        buffer = new byte[length];
    }

    public Bytes(byte[] buff, int offset, int len) {
        this.buffer = buff;
        this.offset = offset;
        this.head = offset + len;
    }

    public int copy(byte[] copyBuffer, int offset, int len) {
        if (offset != 0 && head + len > buffer.length) {
            System.arraycopy(buffer, offset, buffer, 0, head - offset);
            head -= offset;
            offset = 0;
        }
        int copyLen = len;
        if (copyLen > buffer.length) {
            copyLen = buffer.length;
        }
        System.arraycopy(copyBuffer, offset, buffer, head, copyLen);
        head += len;
        return copyLen;
    }

    public int readInt(int len) {
        int value = 0;
        for (int i = 0; i < len; i++) {
            value *= 0x100;
            value += 0xff & buffer[offset];
            offset++;
        }
        return value;
    }

    public static void writeInt(byte[] buffer, int offset, int len, int value) {
        for (int i = 1; i <= len; i++) {
            buffer[offset + len - i] = (byte) value;
            value /= 0x100;
        }
    }

    public String readStr() {
        for (int i=0; i < head; i++) {
            if (buffer[i] == 0) {
                String str = new String(buffer, 0, i, StandardCharsets.UTF_8);
                System.arraycopy(buffer, i, buffer, 0, head-i);
                head -= i;
                return str;
            }
        }
        return null;
    }

    public static void writeStr(Writer writer, String str) throws IOException {
        writer.write(str.getBytes(StandardCharsets.UTF_8));
    }

    public Boolean readBool() {
        if (true) {
            return null;
        }
        return null;
    }

    public static void writeBool(Writer writer, boolean bool) throws IOException {
        byte[] buffer = new byte[] {bool ? (byte)1 : (byte)0};
        writer.write(buffer);
    }
}
