package de.hpi.swa.trufflesqueak.util;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayDeque;
import java.util.Deque;

public class SqueakKeyboard implements KeyListener {

    /**
     * The size of the character queue.
     */
    private static final int TYPEAHEAD_LIMIT = 8;

    private static final int SHIFT_KEY = 8;
    private static final int CONTROL_KEY = 16;
    private static final int COMMAND_KEY = 64;

    /**
     * See ParagraphEditor class>>initializeCmdKeyShortcuts.
     */
    private static final char CURSOR_HOME = 1,
                    CURSOR_END = 4,
                    CR_WITH_IDENT = 13,
                    SELECT_CURRENT_TYPE_IN = 27, // TODO: Interesting, find out what it does
                    CURSOR_LEFT = 28,
                    CURSOR_RIGHT = 29,
                    CURSOR_UP = 30,
                    CURSOR_DOWN = 31;

    /**
     * Squeak keys that do not map to Java key events. Order *MUST* match that of JAVA_KEYS.
     */
    private static final char[] SQUEAK_KEYS = {
                    CURSOR_HOME,
                    CURSOR_END,
                    CR_WITH_IDENT,
                    CURSOR_LEFT,
                    CURSOR_RIGHT,
                    CURSOR_UP,
                    CURSOR_DOWN,
                    SELECT_CURRENT_TYPE_IN,
    };

    /**
     * Counterpart of squeak keys. Order *MUST* match that of SQUEAK_KEYS.
     */
    private static final int[] JAVA_KEYS = {
                    KeyEvent.VK_HOME,
                    KeyEvent.VK_END,
                    KeyEvent.VK_ENTER,
                    KeyEvent.VK_LEFT,
                    KeyEvent.VK_RIGHT,
                    KeyEvent.VK_UP,
                    KeyEvent.VK_DOWN,
                    KeyEvent.VK_BACK_QUOTE,
    };

    private final Deque<Character> keys = new ArrayDeque<>(TYPEAHEAD_LIMIT);
    private int modifierKeys = 0;

    public int nextKey() {
        return keycode(keys.removeFirst());
    }

    public int peekKey() {
        return keys.isEmpty() ? 0 : keycode(keys.peek());
    }

    public int modifierKeys() {
        return modifierKeys;
    }

    public void keyPressed(KeyEvent e) {
        modifierKeys = mapModifierKey(e);
        char keyChar = mapSpecialKey(e);
        if (keyChar != KeyEvent.CHAR_UNDEFINED) {
            enqueue(keyChar);
        }
    }

    public void keyReleased(KeyEvent e) {
        modifierKeys = mapModifierKey(e);
    }

    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == '\n') { // Ignore the return key, mapSpecialKey() took care of it
            return;
        }
        enqueue(e.getKeyChar());
    }

    private void enqueue(char keyChar) {
        if (keys.size() < TYPEAHEAD_LIMIT) {
            keys.add(keyChar);
        }
    }

    private static int mapModifierKey(KeyEvent e) {
        int modifiers = 0;
        if (e.isShiftDown())
            modifiers |= SHIFT_KEY;
        if (e.isControlDown())
            modifiers |= CONTROL_KEY;
        if (e.isAltDown() || e.isMetaDown())
            modifiers |= COMMAND_KEY;

        return modifiers;
    }

    private static char mapSpecialKey(KeyEvent e) {
        int specialKeyIndex = 0;
        while (specialKeyIndex < JAVA_KEYS.length && JAVA_KEYS[specialKeyIndex] != e.getKeyCode())
            specialKeyIndex++;
        if (specialKeyIndex < JAVA_KEYS.length)
            return SQUEAK_KEYS[specialKeyIndex];

        if (e.isAltDown())
            return Character.toLowerCase((char) e.getKeyCode());

        return KeyEvent.CHAR_UNDEFINED;
    }

    private static int keycode(Character c) {
        return c.charValue() & 255;
    }
}
