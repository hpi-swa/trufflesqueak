package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.interop.TruffleObject;

/*
 * Represents not provided values to enable optional arguments in specializations.
 */
public final class NotProvided implements TruffleObject {

    public static final NotProvided INSTANCE = new NotProvided();

    private NotProvided() {
    }

    public static boolean isInstance(final Object obj) {
        return obj == INSTANCE;
    }
}
