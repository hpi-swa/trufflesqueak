package de.hpi.swa.graal.squeak.io;

import de.hpi.swa.graal.squeak.model.PointersObject;

public interface SqueakDisplayInterface {

    void forceRect(int left, int right, int top, int bottom);

    void forceUpdate();

    void close();

    void adjustDisplay(long depth, long width, long height, boolean fullscreen);

    void resizeTo(int width, int height);

    void setFullscreen(boolean enable);

    void open(PointersObject sqDisplay);

    DisplayPoint getLastMousePosition();

    int getLastMouseButton();

    int keyboardPeek();

    int keyboardNext();

    void setCursor(int[] cursorWords, int[] mask, int depth);

    long[] getNextEvent();

    void setDeferUpdates(boolean flag);

    void setWindowTitle(String title);

    void setInputSemaphoreIndex(int interruptSemaphoreIndex);

    String getClipboardData();

    void setClipboardData(String text);

    void beep();

    void pollEvents();
}
