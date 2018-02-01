package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.instrumentation.BaseSqueakObjectMessageResolutionForeign;

public final class FrameMarker implements TruffleObject {

    @Override
    public String toString() {
        return "FrameMarker@" + Integer.toHexString(System.identityHashCode(this));
    }

    public ForeignAccess getForeignAccess() {
        return BaseSqueakObjectMessageResolutionForeign.ACCESS;
    }
}
