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
    private final SqueakDisplay display;
    private final Deque<Integer> keys = new ArrayDeque<>(TYPEAHEAD_LIMIT);

    public SqueakKeyboard(final SqueakDisplay display) {
        this.display = display;
    }

    public int nextKey() {
        return keys.isEmpty() ? 0 : keys.removeFirst();
    }

    public int peekKey() {
        return keys.isEmpty() ? 0 : keys.peek();
    }

    public void keyTyped(final KeyEvent e) {
        assert e.getExtendedKeyCode() == KeyEvent.VK_UNDEFINED;
    }

    public void keyPressed(final KeyEvent e) {
        display.recordModifiers(e);
        if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) { // Ignore shift, etc.
            recordKeyboardEvent(mapSpecialKeyCode(e));
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
            // No event queue, queue keys the old-fashioned way.
            keys.push(code);
        }
    }

    private static int mapSpecialKeyCode(final KeyEvent e) {
        //@formatter:off
        switch(e.getExtendedKeyCode()) {
            case KeyEvent.VK_BACK_SPACE: return 8;
            case KeyEvent.VK_TAB: return 9;
            case KeyEvent.VK_ENTER: return 13;
            case KeyEvent.VK_ESCAPE: return 27;
            case KeyEvent.VK_SPACE: return 32;
            case KeyEvent.VK_PAGE_UP: return 11;
            case KeyEvent.VK_PAGE_DOWN: return 12;
            case KeyEvent.VK_END: return 4;
            case KeyEvent.VK_HOME: return 1;
            case KeyEvent.VK_LEFT: return 28;
            case KeyEvent.VK_UP: return 30;
            case KeyEvent.VK_RIGHT: return 29;
            case KeyEvent.VK_DOWN: return 31;
            case KeyEvent.VK_INSERT: return 5;
            case KeyEvent.VK_DELETE: return 127;
            default: return e.getKeyChar(); // Not a special key.
        }
        //@formatter:on
    }
}
