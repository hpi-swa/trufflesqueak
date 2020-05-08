/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.io;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.event.MouseInputAdapter;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.MOUSE_EVENT;

public final class SqueakMouse extends MouseInputAdapter {
    private final SqueakDisplay display;
    private Point position = new Point(0, 0);

    public SqueakMouse(final SqueakDisplay display) {
        this.display = display;
    }

    public Point getPosition() {
        return position;
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
        int buttons = display.buttons & MOUSE.ALL;
        switch (type) {
            case DOWN:
                buttons = mapButton(e);
                break;
            case MOVE:
                break; // Nothing more to do.
            case UP:
                buttons = 0;
                break;
            default:
                throw SqueakException.create("Unknown mouse event:", e);
        }

        display.buttons = buttons | display.recordModifiers(e);
        display.addEvent(EVENT_TYPE.MOUSE, e.getX(), e.getY(), display.buttons & MOUSE.ALL, display.buttons >> 3);
    }

    private static int mapButton(final MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                return e.isAltDown() ? MOUSE.YELLOW : MOUSE.RED; // left
            case MouseEvent.BUTTON2:
                return MOUSE.YELLOW; // middle
            case MouseEvent.BUTTON3:
                return MOUSE.BLUE; // right
            case MouseEvent.NOBUTTON:
                return 0;
            default:
                throw SqueakException.create("Unknown mouse button in event:", e);
        }
    }
}
