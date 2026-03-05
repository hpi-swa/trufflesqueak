package de.hpi.swa.trufflesqueak.io;

import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD_EVENT;
import org.lwjgl.sdl.SDLKeycode;

public final class SqueakKeyboard {
    private final SqueakDisplay display;

    public SqueakKeyboard(final SqueakDisplay display) {
        this.display = display;
    }

    public void processKeyDown(final int sdlKeySym, final int sdlModifiers) {
        display.recordModifiers(sdlModifiers);

        if (isModifier(sdlKeySym)) {
            return;
        }

        final int keyChar = toSqueakKey(sdlKeySym);

        // Always fire the physical key-down event
        addKeyboardEvent(KEYBOARD_EVENT.DOWN, keyChar);

        // Detect if this is a shortcut (Command, Control, or Option/Alt is held)
        final boolean isShortcut = (sdlModifiers & (SDLKeycode.SDL_KMOD_LCTRL | SDLKeycode.SDL_KMOD_RCTRL |
                SDLKeycode.SDL_KMOD_LGUI | SDLKeycode.SDL_KMOD_RGUI |
                SDLKeycode.SDL_KMOD_LALT | SDLKeycode.SDL_KMOD_RALT)) != 0;

        // If it's a control key (Enter/Backspace) OR a Shortcut, TEXT_INPUT will fail.
        // We must manually emit the CHAR event right now using the base ASCII character.
        if (isControlKey(sdlKeySym) || isShortcut) {
            if (keyChar <= 255) {
                addKeyboardEvent(KEYBOARD_EVENT.CHAR, keyChar);
            }
        }

        // Handle TruffleSqueak's hard-interrupt (Cmd/Alt + '.')
        if (isShortcut && keyChar == '.') {
            display.image.interrupt.setInterruptPending();
        }
    }

    public void processKeyUp(final int sdlKeySym, final int sdlModifiers) {
        display.recordModifiers(sdlModifiers);

        if (isModifier(sdlKeySym)) {
            return;
        }

        final int keyChar = toSqueakKey(sdlKeySym);
        addKeyboardEvent(KEYBOARD_EVENT.UP, keyChar);
    }

    public void processTextInput(final String text) {
        // If a shortcut modifier is held down, we already handled the base character in processKeyDown.
        // We abort here to prevent Mac OS from injecting weird Unicode symbols (like 'π' for Option+p).
        final int currentModifiers = display.buttons >> 3;
        final boolean isShortcut = (currentModifiers & (KEYBOARD.CTRL | KEYBOARD.CMD | KEYBOARD.ALT)) != 0;

        if (isShortcut || text == null || text.isEmpty()) {
            return;
        }

        // For normal typing (including Shift+Number symbols), pipe the translated text directly to Squeak.
        for (int i = 0; i < text.length(); i++) {
            addKeyboardEvent(KEYBOARD_EVENT.CHAR, text.charAt(i));
        }
    }

    private void addKeyboardEvent(final long eventType, final int keyCharOrCode) {
        display.addEvent(EVENT_TYPE.KEYBOARD, keyCharOrCode, eventType, display.buttons >> 3, keyCharOrCode);
    }

    private boolean isModifier(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLKeycode.SDLK_LSHIFT, SDLKeycode.SDLK_RSHIFT,
                 SDLKeycode.SDLK_LCTRL, SDLKeycode.SDLK_RCTRL,
                 SDLKeycode.SDLK_LALT, SDLKeycode.SDLK_RALT,
                 SDLKeycode.SDLK_LGUI, SDLKeycode.SDLK_RGUI -> true;
            default -> false;
        };
    }

    private boolean isControlKey(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLKeycode.SDLK_BACKSPACE, SDLKeycode.SDLK_TAB,
                 SDLKeycode.SDLK_RETURN, SDLKeycode.SDLK_KP_ENTER,
                 SDLKeycode.SDLK_ESCAPE, SDLKeycode.SDLK_PAGEUP,
                 SDLKeycode.SDLK_PAGEDOWN, SDLKeycode.SDLK_END,
                 SDLKeycode.SDLK_HOME, SDLKeycode.SDLK_LEFT,
                 SDLKeycode.SDLK_UP, SDLKeycode.SDLK_RIGHT,
                 SDLKeycode.SDLK_DOWN, SDLKeycode.SDLK_INSERT,
                 SDLKeycode.SDLK_DELETE -> true;
            default -> false;
        };
    }

    private static int toSqueakKey(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLKeycode.SDLK_BACKSPACE -> 8;
            case SDLKeycode.SDLK_TAB -> 9;
            case SDLKeycode.SDLK_RETURN, SDLKeycode.SDLK_KP_ENTER -> 13;
            case SDLKeycode.SDLK_ESCAPE -> 27;
            case SDLKeycode.SDLK_SPACE -> 32;
            case SDLKeycode.SDLK_PAGEUP -> 11;
            case SDLKeycode.SDLK_PAGEDOWN -> 12;
            case SDLKeycode.SDLK_END -> 4;
            case SDLKeycode.SDLK_HOME -> 1;
            case SDLKeycode.SDLK_LEFT -> 28;
            case SDLKeycode.SDLK_UP -> 30;
            case SDLKeycode.SDLK_RIGHT -> 29;
            case SDLKeycode.SDLK_DOWN -> 31;
            case SDLKeycode.SDLK_INSERT -> 5;
            case SDLKeycode.SDLK_DELETE -> 127;

            // Keypad mappings
            case SDLKeycode.SDLK_KP_0 -> '0';
            case SDLKeycode.SDLK_KP_1 -> '1';
            case SDLKeycode.SDLK_KP_2 -> '2';
            case SDLKeycode.SDLK_KP_3 -> '3';
            case SDLKeycode.SDLK_KP_4 -> '4';
            case SDLKeycode.SDLK_KP_5 -> '5';
            case SDLKeycode.SDLK_KP_6 -> '6';
            case SDLKeycode.SDLK_KP_7 -> '7';
            case SDLKeycode.SDLK_KP_8 -> '8';
            case SDLKeycode.SDLK_KP_9 -> '9';
            case SDLKeycode.SDLK_KP_DIVIDE -> '/';
            case SDLKeycode.SDLK_KP_MULTIPLY -> '*';
            case SDLKeycode.SDLK_KP_MINUS -> '-';
            case SDLKeycode.SDLK_KP_PLUS -> '+';
            case SDLKeycode.SDLK_KP_PERIOD -> '.';

            default -> sdlKeySym;
        };
    }
}