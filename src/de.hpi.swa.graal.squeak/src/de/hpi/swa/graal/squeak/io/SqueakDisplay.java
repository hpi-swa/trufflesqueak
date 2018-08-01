package de.hpi.swa.graal.squeak.io;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.PointersObject;

public abstract class SqueakDisplay {

    public static SqueakDisplay create(final SqueakImageContext image, final boolean isHeadless) {
        if (!GraphicsEnvironment.isHeadless() && !isHeadless) {
            return new SqueakDisplayJFrame(image);
        } else {
            return new SqueakDisplayNull();
        }
    }

    public abstract void forceRect(int left, int right, int top, int bottom);

    public abstract Dimension getSize();

    public abstract void setFullscreen(boolean enable);

    public abstract void forceUpdate();

    public abstract void open();

    public abstract void close();

    public abstract void setSqDisplay(PointersObject sqDisplay);

    public abstract Point getLastMousePosition();

    public abstract int getLastMouseButton();

    public abstract int keyboardPeek();

    public abstract int keyboardNext();

    public abstract boolean isHeadless();

    public abstract void setCursor(int[] cursorWords, int depth);

    public abstract long[] getNextEvent();

    public abstract void setDeferUpdates(boolean flag);

    public abstract void adjustDisplay(long depth, long width, long height, boolean fullscreen);

    public abstract void resizeTo(int width, int height);

    public abstract void setWindowTitle(String title);

    public abstract void setInputSemaphoreIndex(int interruptSemaphoreIndex);
}
