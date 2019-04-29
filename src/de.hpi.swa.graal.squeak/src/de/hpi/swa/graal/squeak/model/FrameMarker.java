package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameMarker {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, FrameMarker.class);

    public FrameMarker(final Frame frame) {
        LOG.fine(() -> String.format("new %s; frame args: %s", toString(),
                        ArrayUtils.toJoinedString(", ", frame != null ? frame.getArguments() : ArrayUtils.EMPTY_ARRAY)));
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "FrameMarker@" + Integer.toHexString(System.identityHashCode(this));
    }

    public ContextObject getMaterializedContext() {
        final Frame targetFrame = FrameAccess.findFrameForMarker(this);
        if (targetFrame == null) {
            throw SqueakException.create("Could not find frame for:", this);
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
}
