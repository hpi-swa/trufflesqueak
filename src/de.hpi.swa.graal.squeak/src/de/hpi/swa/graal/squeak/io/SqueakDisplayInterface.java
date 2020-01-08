/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.io;

import de.hpi.swa.graal.squeak.model.PointersObject;

public interface SqueakDisplayInterface {

    void showDisplayBitsLeftTopRightBottom(PointersObject destForm, int left, int top, int right, int bottom);

    void showDisplayRect(int left, int right, int top, int bottom);

    void close();

    void resizeTo(int width, int height);

    DisplayPoint getWindowSize();

    void setFullscreen(boolean enable);

    void open(PointersObject sqDisplay);

    boolean isVisible();

    void setCursor(int[] cursorWords, int[] mask, int width, int height, int depth);

    long[] getNextEvent();

    void setDeferUpdates(boolean flag);

    boolean getDeferUpdates();

    void setWindowTitle(String title);

    void setInputSemaphoreIndex(int interruptSemaphoreIndex);

    String getClipboardData();

    void setClipboardData(String text);

    void beep();

    void pollEvents();
}
