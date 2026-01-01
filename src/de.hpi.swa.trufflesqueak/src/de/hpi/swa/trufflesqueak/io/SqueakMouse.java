/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.event.MouseInputAdapter;

import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE_EVENT;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class SqueakMouse extends MouseInputAdapter {
    private final SqueakDisplay display;

    public SqueakMouse(final SqueakDisplay display) {
        this.display = display;
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        recordMouseEvent(MOUSE_EVENT.MOVE, e);
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        recordMouseEvent(MOUSE_EVENT.MOVE, e);
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        recordMouseEvent(MOUSE_EVENT.DOWN, e);
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        recordMouseEvent(MOUSE_EVENT.UP, e);
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        display.addEvent(EVENT_TYPE.MOUSE_WHEEL, 0L /* X-Axis Scrolling is not supported */, (long) (e.getPreciseWheelRotation() * MOUSE.WHEEL_DELTA_FACTOR), display.buttons >> 3, 0L);
    }

    private void recordMouseEvent(final MOUSE_EVENT type, final MouseEvent e) {
        final int buttons = switch (type) {
            case DOWN -> mapButton(e);
            case MOVE -> display.buttons & MOUSE.ALL;
            case UP -> 0;
        };
        display.buttons = buttons | display.recordModifiers(e);
        display.addEvent(EVENT_TYPE.MOUSE, e.getX(), e.getY(), display.buttons & MOUSE.ALL, display.buttons >> 3);
    }

    private static int mapButton(final MouseEvent e) {
        return switch (e.getButton()) {
            case MouseEvent.BUTTON1 -> e.isAltDown() ? MOUSE.YELLOW : MOUSE.RED; // left
            case MouseEvent.BUTTON2 -> MOUSE.YELLOW; // middle
            case MouseEvent.BUTTON3 -> MOUSE.BLUE; // right
            case MouseEvent.NOBUTTON -> 0;
            default -> {
                LogUtils.IO.warning("Unknown mouse button in event: " + e);
                yield 0;
            }
        };
    }
}
