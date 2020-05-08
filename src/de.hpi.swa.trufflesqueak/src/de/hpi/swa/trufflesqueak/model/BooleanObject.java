/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;

public final class BooleanObject {
    public static final boolean FALSE = false;
    public static final boolean TRUE = true;

    private BooleanObject() {
    }

    public static boolean wrap(final boolean object) {
        return object; /** avoid check since true->true and false->false. */
    }

    public static long getFalseSqueakHash() {
        return 2L;
    }

    public static long getTrueSqueakHash() {
        return 3L;
    }

    public static void write(final SqueakImageWriter writer, final boolean value) {
        if (value) {
            writer.writeObjectHeader(0, getTrueSqueakHash(), writer.getImage().trueClass, 0);
        } else {
            writer.writeObjectHeader(0, getFalseSqueakHash(), writer.getImage().falseClass, 0);
        }
        writer.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
    }
}
