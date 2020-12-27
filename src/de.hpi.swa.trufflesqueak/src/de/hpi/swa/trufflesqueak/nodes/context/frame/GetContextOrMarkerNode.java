/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or marker, lazily initializes the latter if necessary. */
public final class GetContextOrMarkerNode extends AbstractNode {
    @CompilationFinal private FrameSlot contextSlot;
    @CompilationFinal private FrameSlot markerSlot;
    private final ConditionProfile hasContextProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasMarkerProfile = ConditionProfile.createBinaryProfile();

    public static GetContextOrMarkerNode create() {
        return new GetContextOrMarkerNode();
    }

    public Object execute(final VirtualFrame frame) {
        if (contextSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            contextSlot = FrameAccess.getContextSlot(frame);
            markerSlot = FrameAccess.getMarkerSlot(frame);
        }
        final Object contextOrNil = FrameAccess.getContextOrNil(frame, contextSlot);
        if (hasContextProfile.profile(contextOrNil != NilObject.SINGLETON)) {
            return contextOrNil;
        } else {
            final Object marker = FrameAccess.getMarkerOrNil(frame, markerSlot);
            if (hasMarkerProfile.profile(marker != NilObject.SINGLETON)) {
                return marker;
            } else {
                final FrameMarker newMarker = new FrameMarker();
                FrameAccess.setMarker(frame, markerSlot, newMarker);
                return newMarker;
            }
        }
    }

    public static Object getNotProfiled(final VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        final Object contextOrNil = FrameAccess.getContextOrNilSlow(frame);
        if (contextOrNil != NilObject.SINGLETON) {
            return contextOrNil;
        } else {
            final FrameSlot markerSlot = FrameAccess.getMarkerSlot(frame);
            final Object marker = FrameAccess.getMarkerOrNil(frame, markerSlot);
            if (marker != NilObject.SINGLETON) {
                return marker;
            } else {
                final FrameMarker newMarker = new FrameMarker();
                FrameAccess.setMarker(frame, markerSlot, newMarker);
                return newMarker;
            }
        }
    }
}
