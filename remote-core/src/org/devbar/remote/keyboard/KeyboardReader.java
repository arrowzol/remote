package org.devbar.remote.keyboard;

public interface KeyboardReader {
    void startKeyboardFocus();
    default void endKeyboardFocus() {}
    boolean keyboardInput(String line);
}
