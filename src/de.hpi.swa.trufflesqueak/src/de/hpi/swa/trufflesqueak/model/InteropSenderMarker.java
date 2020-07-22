/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

public final class InteropSenderMarker extends AbstractSqueakObject {
    public static final InteropSenderMarker SINGLETON = new InteropSenderMarker();

    private InteropSenderMarker() {
    }

    @Override
    public int getNumSlots() {
        return 0;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return "interopSenderMarker";
    }
}
