package de.hpi.swa.trufflesqueak.util;

import java.awt.Dimension;
import java.awt.Point;

public class BaseDisplay {
    @SuppressWarnings("unused")
    public void drawRect(int left, int right, int top, int bottom) {
    }

    public Dimension getSize() {
        return new Dimension(0, 0);
    }

    public int getButtons() {
        return 0;
    }

    public Point getMousePosition() {
        return new Point(0, 0);
    }

    public void setFullscreen(@SuppressWarnings("unused") boolean enable) {
    }

    public void forceUpdate() {
    }

    public int nextKey() {
        return 0;
    }

    public int peekKey() {
        return 0;
    }

    public void open() {
    }
}
