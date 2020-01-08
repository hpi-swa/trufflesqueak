/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

public final class BooleanObject {
    public static final boolean FALSE = false;
    public static final boolean TRUE = true;

    private BooleanObject() {
    }

    public static boolean wrap(final boolean object) {
        return object; /** avoid check since true->true and false->false. */
    }
}
