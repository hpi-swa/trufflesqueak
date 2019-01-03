package de.hpi.swa.graal.squeak.model;

import java.io.PrintStream;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.interop.SqueakObjectMessageResolutionForeign;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameMarker implements TruffleObject {
    private static final boolean LOG_ALLOCATIONS = false;

    public FrameMarker(final Frame frame) {
        if (LOG_ALLOCATIONS) {
            logAllocations(frame != null ? frame.getArguments() : ArrayUtils.EMPTY_ARRAY);
        }
    }

    @TruffleBoundary
    private void logAllocations(final Object[] arguments) {
        final PrintStream err = System.err;
        err.println(String.format("new %s; frame args: %s", toString(), ArrayUtils.toJoinedString(", ", arguments)));
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "FrameMarker@" + Integer.toHexString(System.identityHashCode(this));
    }

    public ContextObject getMaterializedContext() {
        final Frame targetFrame = FrameAccess.findFrameForMarker(this);
        if (targetFrame == null) {
            throw new SqueakException("Could not find frame for: " + this);
        }
        final ContextObject context = FrameAccess.getContext(targetFrame);
        if (context != null) {
            assert context.getFrameMarker() == this;
            return context;
        } else {
            assert this == FrameAccess.getMarker(targetFrame) : "Frame does not match";
            return ContextObject.create(targetFrame);
        }
    }

    public ForeignAccess getForeignAccess() {
        return SqueakObjectMessageResolutionForeign.ACCESS;
    }
}
