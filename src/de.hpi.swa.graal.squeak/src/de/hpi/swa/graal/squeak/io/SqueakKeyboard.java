/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.io;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import de.hpi.swa.graal.squeak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD_EVENT;

public final class SqueakKeyboard implements KeyListener {
    private final SqueakDisplay display;

    public SqueakKeyboard(final SqueakDisplay display) {
        this.display = display;
    }

    @Override
    public void keyTyped(final KeyEvent e) {
        // Ignored.
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        display.recordModifiers(e);
        final int keyCode = mapSpecialKeyCode(e);
        final char keyChar = e.getKeyChar();
        if (keyCode >= 0) {
            // Special key pressed, record mapped code
            recordKeyboardEvent(keyCode);
        } else if (keyChar != KeyEvent.CHAR_UNDEFINED) {
            // Regular key pressed, record char value
            recordKeyboardEvent(keyChar);
        }
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        display.recordModifiers(e);
    }

    private void recordKeyboardEvent(final int key) {
        final int buttonsShifted = display.buttons >> 3;
        final int code = buttonsShifted << 8 | key;
        if (code == KEYBOARD.INTERRUPT_KEYCODE) {
            display.image.interrupt.setInterruptPending();
        } else {
            display.addEvent(EVENT_TYPE.KEYBOARD, key, KEYBOARD_EVENT.CHAR, buttonsShifted, key);
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
            default: return -1; // Not a special key.
        }
        //@formatter:on
    }
}
