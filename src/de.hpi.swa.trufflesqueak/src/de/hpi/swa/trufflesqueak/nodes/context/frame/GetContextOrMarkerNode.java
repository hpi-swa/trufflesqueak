/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or marker, lazily initializes the latter if necessary. */
public final class GetContextOrMarkerNode extends AbstractNode {
    private final ConditionProfile hasContextProfile = ConditionProfile.create();
    private final ConditionProfile hasMarkerProfile = ConditionProfile.create();

    @NeverDefault
    public static GetContextOrMarkerNode create() {
        return new GetContextOrMarkerNode();
    }

    public Object execute(final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (hasContextProfile.profile(context != null)) {
            return context;
        } else {
            final FrameMarker marker = FrameAccess.getMarker(frame);
            if (hasMarkerProfile.profile(marker != null)) {
                return marker;
            } else {
                final FrameMarker newMarker = new FrameMarker();
                FrameAccess.setMarker(frame, newMarker);
                return newMarker;
            }
        }
    }

    public static Object getNotProfiled(final VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        final ContextObject context = FrameAccess.getContext(frame);
        if (context != null) {
            return context;
        } else {
            final FrameMarker marker = FrameAccess.getMarker(frame);
            if (marker != null) {
                return marker;
            } else {
                final FrameMarker newMarker = new FrameMarker();
                FrameAccess.setMarker(frame, newMarker);
                return newMarker;
            }
        }
    }
}
