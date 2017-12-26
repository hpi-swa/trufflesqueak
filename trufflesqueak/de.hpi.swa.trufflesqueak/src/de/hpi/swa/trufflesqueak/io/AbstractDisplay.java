package de.hpi.swa.trufflesqueak.io;

import java.awt.Dimension;
import java.awt.Point;

public abstract class AbstractDisplay {
    public abstract void drawRect(int left, int right, int top, int bottom);

    public abstract Dimension getSize();

    public abstract int getButtons();

    public abstract Point getMousePosition();

    public abstract void setFullscreen(boolean enable);

    public abstract void forceUpdate();

    public abstract int nextKey();

    public abstract int peekKey();

    public abstract void open();
}
