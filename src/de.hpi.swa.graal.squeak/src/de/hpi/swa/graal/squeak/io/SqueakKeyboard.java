package de.hpi.swa.graal.squeak.io;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayDeque;
import java.util.Deque;

import de.hpi.swa.graal.squeak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD_EVENT;

public final class SqueakKeyboard implements KeyListener {
    private static final int TYPEAHEAD_LIMIT = 8;
    private final SqueakDisplayJFrame display;
    private final Deque<Integer> keys = new ArrayDeque<>(TYPEAHEAD_LIMIT);

    public SqueakKeyboard(final SqueakDisplayJFrame display) {
        this.display = display;
    }

    public int nextKey() {
        return keys.isEmpty() ? 0 : keys.removeFirst();
    }

    public int peekKey() {
        return keys.isEmpty() ? 0 : keys.peek();
    }

    public void keyTyped(final KeyEvent e) {
        display.recordModifiers(e);
        if (mapSqueakCode(e) < 0) {
            recordKeyboardEvent(e.getKeyChar());
        }
    }

    public void keyPressed(final KeyEvent e) {
        display.recordModifiers(e);
        final int squeakCode = mapSqueakCode(e);
        if (squeakCode >= 0) {
            recordKeyboardEvent(squeakCode);
        } else if ((e.isMetaDown() || (e.isAltDown() && !e.isControlDown())) && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
            recordKeyboardEvent(e.getKeyChar());
        }
    }

    public void keyReleased(final KeyEvent e) {
        display.recordModifiers(e);
    }

    private void recordKeyboardEvent(final int key) {
        final int buttonsShifted = display.buttons >> 3;
        final int code = buttonsShifted << 8 | key;
        if (code == KEYBOARD.INTERRUPT_KEYCODE) {
            display.image.interrupt.setInterruptPending();
        } else if (display.usesEventQueue) {
            display.addEvent(EVENT_TYPE.KEYBOARD, key, KEYBOARD_EVENT.CHAR, buttonsShifted, key);
        } else {
            keys.push(code);
        }
    }

    private static int mapSqueakCode(final KeyEvent e) {
        //@formatter:off
        switch(e.getExtendedKeyCode()) {
            case 8: return 8;    // Backspace
            case 9:  return 9;   // Tab
            case 13: return 13;  // Return
            case 27: return 27;  // Escape
            case 32: return 32;  // Space
            case 33: return 11;  // PageUp
            case 34: return 12;  // PageDown
            case 35: return 4;   // End
            case 36: return 1;   // Home
            case 37: return 28;  // Left
            case 38: return 30;  // Up
            case 39: return 29;  // Right
            case 40: return 31;  // Down
            case 45: return 5;   // Insert
            case 46: return 127; // Delete
            default: return -1;
        }
        //@formatter:on
    }
}
