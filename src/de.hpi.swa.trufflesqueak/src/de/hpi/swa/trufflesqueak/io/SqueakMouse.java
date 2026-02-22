/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE_EVENT;
import io.github.humbleui.jwm.EventMouseButton;
import io.github.humbleui.jwm.EventMouseMove;
import io.github.humbleui.jwm.EventMouseScroll;
import io.github.humbleui.jwm.KeyModifier;
import io.github.humbleui.jwm.MouseButton;

public final class SqueakMouse {
    private final SqueakDisplay display;

    public SqueakMouse(final SqueakDisplay display) {
        this.display = display;
    }

    public void onMove(final EventMouseMove e) {
        final int squeakModifiers = SqueakKeyboard.mapModifiers(
                        e.isModifierDown(KeyModifier.SHIFT),
                        e.isModifierDown(KeyModifier.CONTROL),
                        e.isModifierDown(KeyModifier.ALT),
                        e.isModifierDown(KeyModifier.MAC_COMMAND));
        recordMouseEvent(MOUSE_EVENT.MOVE, e.getX(), e.getY(), 0, squeakModifiers);
    }

    public void onButton(final EventMouseButton e) {
        final int squeakModifiers = SqueakKeyboard.mapModifiers(
                        e.isModifierDown(KeyModifier.SHIFT),
                        e.isModifierDown(KeyModifier.CONTROL),
                        e.isModifierDown(KeyModifier.ALT),
                        e.isModifierDown(KeyModifier.MAC_COMMAND));
        final MOUSE_EVENT type = e.isPressed() ? MOUSE_EVENT.DOWN : MOUSE_EVENT.UP;
        final int button = mapButton(e.getButton());
        recordMouseEvent(type, e.getX(), e.getY(), button, squeakModifiers);
    }

    public void onScroll(final EventMouseScroll e) {
        final int squeakModifiers = SqueakKeyboard.mapModifiers(
                        e.isModifierDown(KeyModifier.SHIFT),
                        e.isModifierDown(KeyModifier.CONTROL),
                        e.isModifierDown(KeyModifier.ALT),
                        e.isModifierDown(KeyModifier.MAC_COMMAND));

        // Update the display's global modifier state so Squeak knows what's held during scroll
        display.buttons = (display.buttons & MOUSE.ALL) | squeakModifiers;

        // JWM gives deltaX/Y. Squeak expects Y scroll.
        final long delta = (long) (e.getDeltaY() * MOUSE.WHEEL_DELTA_FACTOR);
        display.addEvent(EVENT_TYPE.MOUSE_WHEEL, 0L, delta, display.buttons >> 3, 0L);
    }

    private void recordMouseEvent(final MOUSE_EVENT type, final int x, final int y, final int buttonChange, final int squeakModifiers) {
        if (type == MOUSE_EVENT.DOWN) {
            display.buttons |= buttonChange;
        } else if (type == MOUSE_EVENT.UP) {
            display.buttons &= ~buttonChange;
        }

        // Apply the modifiers (shifted left by 3 bits per Squeak's layout expectation)
        display.buttons = (display.buttons & MOUSE.ALL) | squeakModifiers;

        // Add the combined mouse event to Squeak's event queue
        display.addEvent(EVENT_TYPE.MOUSE, x, y, display.buttons & MOUSE.ALL, display.buttons >> 3);
    }

    private static int mapButton(final MouseButton button) {
        if (button == MouseButton.PRIMARY) {
            return MOUSE.RED;
        }
        if (button == MouseButton.MIDDLE) {
            return MOUSE.YELLOW;
        }
        return 0;
    }
}
