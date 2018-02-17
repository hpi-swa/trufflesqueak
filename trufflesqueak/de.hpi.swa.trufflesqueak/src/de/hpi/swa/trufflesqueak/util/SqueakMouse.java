package de.hpi.swa.trufflesqueak.util;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputAdapter;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;

public class SqueakMouse extends MouseInputAdapter {
    private Point position;
    private int buttons;

    private final static int RED = 4;
    private final static int YELLOW = 2;
    private final static int BLUE = 1;

    public Point getPosition() {
        return position;
    }

    public int getButtons() {
        return buttons;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        position = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        position = e.getPoint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        buttons |= mapButton(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        buttons &= ~mapButton(e);
    }

    private static int mapButton(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                if (e.isControlDown())
                    return YELLOW;
                if (e.isAltDown())
                    return BLUE;
                return RED;
            case MouseEvent.BUTTON2:
                return BLUE;    // middle (frame menu)
            case MouseEvent.BUTTON3:
                return YELLOW;  // right (pane menu)
            case MouseEvent.NOBUTTON:
                return 0;
        }
        throw new SqueakException("unknown mouse button in event");
    }
}