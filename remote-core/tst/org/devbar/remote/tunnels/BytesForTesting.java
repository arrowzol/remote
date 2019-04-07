package org.devbar.remote.tunnels;

import org.devbar.remote.utils.Bytes;

public class BytesForTesting extends Bytes {

    private int originalHead;

    public BytesForTesting(int maxSize) {
        super(maxSize);
    }

    public void begin() {
        originalHead = head;
        head = tail;
    }

    public boolean advance(int step) {
        if (head == originalHead) {
            return false;
        }
        head += step;
        if (head > originalHead) {
            head = originalHead;
        }
        return true;
    }
}
