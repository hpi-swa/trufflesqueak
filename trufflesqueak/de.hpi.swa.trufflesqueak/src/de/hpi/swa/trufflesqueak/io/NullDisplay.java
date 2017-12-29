package de.hpi.swa.trufflesqueak.io;

import java.awt.Dimension;
import java.awt.Point;

public class NullDisplay extends AbstractDisplay {
    @Override
    public void drawRect(int left, int right, int top, int bottom) {
    }

    @Override
    public Dimension getSize() {
        return new Dimension(0, 0);
    }

    @Override
    public int getButtons() {
        return 0;
    }

    @Override
    public Point getMousePosition() {
        return new Point(0, 0);
    }

    @Override
    public void setFullscreen(boolean enable) {
    }

    @Override
    public void forceUpdate() {
    }

    @Override
    public int nextKey() {
        return 0;
    }

    @Override
    public int peekKey() {
        return 0;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }
}
