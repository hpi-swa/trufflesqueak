/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageConstants;
import de.hpi.swa.graal.squeak.image.SqueakImageWriter;

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

    public static void write(final SqueakImageWriter writerNode, final boolean value) {
        if (value) {
            writerNode.writeObjectHeader(0, getTrueSqueakHash(), writerNode.getImage().trueClass, 0);
        } else {
            writerNode.writeObjectHeader(0, getFalseSqueakHash(), writerNode.getImage().falseClass, 0);
        }
        writerNode.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
    }
}
