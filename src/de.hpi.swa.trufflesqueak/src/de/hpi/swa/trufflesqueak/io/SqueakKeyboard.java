/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD_EVENT;

public final class SqueakKeyboard implements KeyListener {
    private final SqueakDisplay display;

    public SqueakKeyboard(final SqueakDisplay display) {
        this.display = display;
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        display.recordModifiers(e);
        final int keyChar = toKeyChar(e);
        addKeyboardEvent(KEYBOARD_EVENT.DOWN, keyChar != KeyEvent.CHAR_UNDEFINED ? keyChar : e.getKeyCode());
        if (keyChar != KeyEvent.CHAR_UNDEFINED) {
            addKeyboardEvent(KEYBOARD_EVENT.CHAR, keyChar);
        }
        if ((e.isAltDown() || e.isMetaDown()) && keyChar == '.') {
            display.image.interrupt.setInterruptPending();
        }
    }

    @Override
    public void keyTyped(final KeyEvent e) {
        /** Keyboard char events handled in keyPressed(KeyEvent). */
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        display.recordModifiers(e);
        final int keyChar = toKeyChar(e);
        addKeyboardEvent(KEYBOARD_EVENT.UP, keyChar != KeyEvent.CHAR_UNDEFINED ? keyChar : e.getKeyCode());
    }

    private void addKeyboardEvent(final long eventType, final int keyCharOrCode) {
        display.addEvent(EVENT_TYPE.KEYBOARD, keyCharOrCode, eventType, display.buttons >> 3, keyCharOrCode);
    }

    private static int toKeyChar(final KeyEvent e) {
        return switch (e.getKeyCode()) { // Handle special keys.
            case KeyEvent.VK_BACK_SPACE -> 8;
            case KeyEvent.VK_TAB -> 9;
            case KeyEvent.VK_ENTER -> 13;
            case KeyEvent.VK_ESCAPE -> 27;
            case KeyEvent.VK_SPACE -> 32;
            case KeyEvent.VK_PAGE_UP -> 11;
            case KeyEvent.VK_PAGE_DOWN -> 12;
            case KeyEvent.VK_END -> 4;
            case KeyEvent.VK_HOME -> 1;
            case KeyEvent.VK_LEFT -> 28;
            case KeyEvent.VK_UP -> 30;
            case KeyEvent.VK_RIGHT -> 29;
            case KeyEvent.VK_DOWN -> 31;
            case KeyEvent.VK_INSERT -> 5;
            case KeyEvent.VK_DELETE -> 127;
            default -> e.getKeyChar();
        };
    }
}
