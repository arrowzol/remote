package org.devbar.remote.utils;

import org.devbar.remote.agents.Writer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Bytes {
    private byte[] buffer;
    protected int tail;
    protected int head;

    public Bytes(int length) {
        buffer = new byte[length];
    }

    /////////////////////////////////////////////////////////////////
    // I/O Operations
    /////////////////////////////////////////////////////////////////

    public void copy(Bytes bytes) {
        if (empty()) {
            clear();
        }
        int copyLen = bytes.size();
        if (tail != 0 && head + copyLen > buffer.length) {
            System.arraycopy(buffer, tail, buffer, 0, head - tail);
            head -= tail;
            tail = 0;
        }
        if (copyLen > buffer.length - head) {
            throw new IllegalArgumentException("buffer overflow");
        }
        System.arraycopy(bytes.buffer, bytes.tail, buffer, head, copyLen);
        head += copyLen;
    }

    public void copy(byte[] copyBuffer, int offset, int len) {
        if (empty()) {
            clear();
        }
        if (tail != 0 && head + len > buffer.length) {
            System.arraycopy(buffer, tail, buffer, 0, head - tail);
            head -= tail;
            tail = 0;
        }
        int copyLen = len;
        if (copyLen > buffer.length - head) {
            throw new IllegalArgumentException("buffer overflow");
        }
        System.arraycopy(copyBuffer, offset, buffer, head, copyLen);
        head += len;
    }

    public boolean read(InputStream is) throws IOException {
        if (head == tail) {
            clear();
        }
        int len = is.read(buffer, head, buffer.length - head);
        if (len == -1) {
            return false;
        }
        head += len;
        return true;
    }

    public void write(OutputStream os) throws IOException {
        os.write(buffer, tail, head - tail);
    }

    /////////////////////////////////////////////////////////////////
    // Status operations
    /////////////////////////////////////////////////////////////////

    public boolean empty() {
        return head == tail;
    }

    public boolean full() {
        return head == buffer.length && tail == 0;
    }

    public int size() {
        return head - tail;
    }

    /////////////////////////////////////////////////////////////////
    // Size manipulations
    /////////////////////////////////////////////////////////////////

    public long takeSnapshot() {
        return (long)head + ((long)tail << 32);
    }

    public void restoreSnapshot(long snapshot) {
        head = (int)snapshot;
        tail = (int)(snapshot >> 32);
    }

    public void restoreSnapshotHead(long snapshot) {
        head = (int)snapshot;
    }

    public void clear() {
        tail = 0;
        head = 0;
    }

    public int skip(int len) {
        int actualLen = len;
        tail += len;
        if (tail > head) {
            actualLen = head - (tail - len);
            tail = head;
        }
        return actualLen;
    }

    public int setSize(int requestedSize) {
        int oldHead = head;
        head = tail + requestedSize;
        if (head > buffer.length) {
            head = buffer.length;
        }
        return head - tail;
    }

    /////////////////////////////////////////////////////////////////
    // Serialization
    /////////////////////////////////////////////////////////////////

    public String toStr() {
        return new String(buffer, tail, head - tail, StandardCharsets.UTF_8);
    }

    public int readInt(int len) {
        int value = 0;
        for (int i = 0; i < len; i++) {
            value *= 0x100;
            value += 0xff & buffer[tail];
            tail++;
        }
        if (tail > head) {
            tail -= len;
            throw new IllegalArgumentException("buffer overshot");
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
        if (tail == head) {
            return null;
        }
        byte boolByte = buffer[tail];
        tail++;
        if (boolByte == (byte)-1) {
            return true;
        } else if (boolByte == (byte)0) {
            return false;
        }
        throw new IllegalArgumentException("protocol error");
    }

    public static void writeBool(Writer writer, boolean bool) throws IOException {
        byte[] buffer = new byte[] {bool ? (byte)-1 : (byte)0};
        writer.write(buffer);
    }

    @Override
    public String toString() {
        return "B[" + size() + "]";
    }
}
