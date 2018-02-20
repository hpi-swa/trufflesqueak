package de.hpi.swa.trufflesqueak.util;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputAdapter;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakDisplay.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.util.SqueakDisplay.JavaDisplay;

public class SqueakMouse extends MouseInputAdapter {
    @CompilationFinal private final JavaDisplay display;
    private Point position = new Point(0, 0);
    private int buttons = 0;

    private final static int RED = 4;
    private final static int YELLOW = 2;
    private final static int BLUE = 1;

    public SqueakMouse(JavaDisplay display) {
        this.display = display;
    }

    public Point getPosition() {
        return position;
    }

    public int getButtons() {
        return buttons;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        position = e.getPoint();
        addEvent(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        position = e.getPoint();
        addEvent(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        buttons |= mapButton(e);
        addEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        buttons &= ~mapButton(e);
        addEvent(e);
    }

    private void addEvent(MouseEvent e) {
        display.addEvent(new long[]{EVENT_TYPE.MOUSE, display.getEventTime(), e.getX(), e.getY(), buttons, display.keyboard.modifierKeys(), 0, 0});
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