package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.instrumentation.BaseSqueakObjectMessageResolutionForeign;

public final class FrameMarker implements TruffleObject {

    public FrameMarker() {
    }

    @Override
    public String toString() {
        return "aFrameMarker";
    }

    public ForeignAccess getForeignAccess() {
        return BaseSqueakObjectMessageResolutionForeign.ACCESS;
    }
}
