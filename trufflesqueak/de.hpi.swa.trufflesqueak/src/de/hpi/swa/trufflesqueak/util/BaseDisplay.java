package de.hpi.swa.trufflesqueak.util;

import java.awt.Dimension;
import java.awt.Point;

public class BaseDisplay {
    public Dimension getSize() {
        return new Dimension(0, 0);
    }

    public int getButtons() {
        return 0;
    }

    public Point getMousePosition() {
        return new Point(0, 0);
    }
}
