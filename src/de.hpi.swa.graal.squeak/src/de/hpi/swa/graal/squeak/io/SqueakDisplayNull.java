package de.hpi.swa.graal.squeak.io;

import java.awt.Dimension;
import java.awt.Point;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.model.PointersObject;

public final class SqueakDisplayNull extends SqueakDisplay {
    @CompilationFinal private static final Dimension DEFAULT_DIMENSION = new Dimension(1024, 768);
    @CompilationFinal private static final Point NULL_POINT = new Point(0, 0);

    @Override
    public void forceRect(final int left, final int right, final int top, final int bottom) {
        // ignore
    }

    @Override
    public Dimension getSize() {
        return DEFAULT_DIMENSION;
    }

    @Override
    public void setFullscreen(final boolean enable) {
        // ignore
    }

    @Override
    public void forceUpdate() {
        // ignore
    }

    @Override
    public void open() {
        // ignore
    }

    @Override
    public void close() {
        // ignore
    }

    @Override
    public void setSqDisplay(final PointersObject sqDisplay) {
        // ignore
    }

    @Override
    public Point getLastMousePosition() {
        return NULL_POINT;
    }

    @Override
    public int getLastMouseButton() {
        return 0;
    }

    @Override
    public int keyboardPeek() {
        return 0;
    }

    @Override
    public int keyboardNext() {
        return 0;
    }

    @Override
    public boolean isHeadless() {
        return true;
    }

    @Override
    public void setCursor(final int[] cursorWords, final int depth) {
        // ignore
    }

    @Override
    public long[] getNextEvent() {
        return SqueakIOConstants.NULL_EVENT;
    }

    @Override
    public void setDeferUpdates(final boolean flag) {
        // ignore
    }

    @Override
    public void adjustDisplay(final long depth, final long width, final long height, final boolean fullscreen) {
        // ignore
    }

    @Override
    public void resizeTo(final int width, final int height) {
        // ignore
    }

    @Override
    public void setWindowTitle(final String title) {
        // ignore
    }

    @Override
    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        // ignore
    }
}
