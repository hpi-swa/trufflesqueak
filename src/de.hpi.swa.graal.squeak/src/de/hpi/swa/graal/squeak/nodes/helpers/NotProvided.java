package de.hpi.swa.graal.squeak.nodes.helpers;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

public final class NotProvided implements TruffleObject {

    public static final NotProvided INSTANCE = new NotProvided();

    private NotProvided() {
    }

    public static boolean isInstance(final Object obj) {
        return obj instanceof NotProvided;
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

}
