/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD_EVENT;
import io.github.humbleui.jwm.EventKey;
import io.github.humbleui.jwm.EventTextInput;
import io.github.humbleui.jwm.Key;
import io.github.humbleui.jwm.KeyModifier;

public final class SqueakKeyboard {
    private final SqueakDisplay display;

    public SqueakKeyboard(final SqueakDisplay display) {
        this.display = display;
    }

    public void onKey(final EventKey e) {
        final int modifiers = mapModifiers(
                        e.isModifierDown(KeyModifier.SHIFT),
                        e.isModifierDown(KeyModifier.CONTROL),
                        e.isModifierDown(KeyModifier.ALT),
                        e.isModifierDown(KeyModifier.MAC_COMMAND));

        final int keyCharOrCode = toSqueakKeyCode(e.getKey());
        final boolean isCmd = (modifiers & KEYBOARD.CMD) != 0;
        final boolean isAlt = (modifiers & KEYBOARD.ALT) != 0;
        final boolean isCtrl = (modifiers & KEYBOARD.CTRL) != 0;

        // If Cmd+. (Mac) or Alt+. (Windows/Linux) is pressed, notify the VM.
        if ((isAlt || isCmd) && keyCharOrCode == '.') {
            // Signal on key up since macOS swallows the key down.
            if (!e.isPressed()) {
                display.image.interrupt.setInterruptPending();
            }
        }

        // Update display buttons/modifiers
        display.buttons = (display.buttons & ~KEYBOARD.ALL) | modifiers;

        if (e.isPressed()) {
            addKeyboardEvent(KEYBOARD_EVENT.DOWN, keyCharOrCode);

            // Generate missing CHAR events for shortcuts and control keys.
            // The OS won't trigger onTextInput for these, so we must do it manually.
            if (isCmd || isCtrl || isAlt || isControlKey(e.getKey())) {
                addKeyboardEvent(KEYBOARD_EVENT.CHAR, keyCharOrCode);
            }

        } else {
            addKeyboardEvent(KEYBOARD_EVENT.UP, keyCharOrCode);
        }
    }

    private static boolean isControlKey(final Key key) {
        switch (key) {
            case BACKSPACE:
            case DELETE:
            case TAB:
            case ENTER:
            case ESCAPE:
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
            case HOME:
            case END:
            case PAGE_UP:
            case PAGE_DOWN:
                return true;
            default:
                return false;
        }
    }

    public void onTextInput(final EventTextInput e) {
        final String text = e.getText();
        for (int i = 0; i < text.length(); i++) {
            final int charCode = text.charAt(i);
            // Send the exact Unicode character the OS generated
            addKeyboardEvent(KEYBOARD_EVENT.CHAR, charCode);
        }
    }

    private void addKeyboardEvent(final long eventType, final int keyCharOrCode) {
        display.addEvent(EVENT_TYPE.KEYBOARD, keyCharOrCode, eventType, display.buttons >> 3, keyCharOrCode);
    }

    public static int mapModifiers(final boolean isShift, final boolean isCtrl, final boolean isAlt, final boolean isCmd) {
        int s = 0;
        if (isShift) {
            s |= KEYBOARD.SHIFT;
        }
        if (isCtrl) {
            s |= KEYBOARD.CTRL;
        }
        if (isAlt) {
            s |= KEYBOARD.ALT;
        }
        if (isCmd) {
            s |= KEYBOARD.CMD;
        }
        return s;
    }

    private static int toSqueakKeyCode(final Key key) {
        switch (key) {
            case BACKSPACE:
                return 8;
            case TAB:
                return 9;
            case ENTER:
                return 13;
            case ESCAPE:
                return 27;
            case SPACE:
                return 32;
            case PAGE_UP:
                return 11;
            case PAGE_DOWN:
                return 12;
            case END:
                return 4;
            case HOME:
                return 1;
            case LEFT:
                return 28;
            case UP:
                return 30;
            case RIGHT:
                return 29;
            case DOWN:
                return 31;
            case INSERT:
                return 5;
            case DELETE:
                return 127;
            case COMMA:
                return ',';
            case PERIOD:
                return '.';
            case SLASH:
                return '/';
            case SEMICOLON:
                return ';';
            case MINUS:
                return '-';
            case EQUALS:
                return '=';
            case OPEN_BRACKET:
                return '[';
            case CLOSE_BRACKET:
                return ']';
            default:
                final String name = key.name();
                // Map JWM's "A" through "Z" to ASCII a-z
                if (name.length() == 1 && name.charAt(0) >= 'A' && name.charAt(0) <= 'Z') {
                    return Character.toLowerCase(name.charAt(0));
                }
                // Map JWM's "DIGIT1", "DIGIT2", etc. to ASCII 0-9
                if (name.startsWith("DIGIT") && name.length() == 6) {
                    return name.charAt(5);
                }
                return 0; // Ignore unknown/modifier keys in the char stream
        }
    }
}
