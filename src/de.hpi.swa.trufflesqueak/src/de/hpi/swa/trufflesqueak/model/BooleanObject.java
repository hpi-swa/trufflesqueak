/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;

public final class BooleanObject {
    public static final boolean FALSE = false;
    public static final boolean TRUE = true;
    public static final long FALSE_SQUEAK_HASH = 2L;
    public static final long TRUE_SQUEAK_HASH = 3L;

    private BooleanObject() {
    }

    public static boolean wrap(final boolean object) {
        return object; /** avoid check since true->true and false->false. */
    }

    public static void write(final SqueakImageWriter writer, final boolean value) {
        if (value) {
            writer.writeObjectHeader(0, TRUE_SQUEAK_HASH, writer.getImage().trueClass, 0);
        } else {
            writer.writeObjectHeader(0, FALSE_SQUEAK_HASH, writer.getImage().falseClass, 0);
        }
        writer.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
    }
}
