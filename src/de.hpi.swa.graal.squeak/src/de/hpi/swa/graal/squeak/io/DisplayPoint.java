/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.io;

import com.oracle.truffle.api.CompilerDirectives.ValueType;

@ValueType
public final class DisplayPoint {
    private final int width;
    private final int height;

    public DisplayPoint(final int w, final int h) {
        width = w;
        height = h;
    }

    public DisplayPoint(final double w, final double h) {
        width = (int) w;
        height = (int) h;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
