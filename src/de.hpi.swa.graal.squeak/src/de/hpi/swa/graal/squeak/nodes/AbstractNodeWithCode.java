/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class AbstractNodeWithCode extends AbstractNode {
    protected final CompiledCodeObject code;
    @CompilationFinal private ConditionProfile hasContextProfile;
    @CompilationFinal private ConditionProfile hasMarkerProfile;

    protected AbstractNodeWithCode(final CompiledCodeObject code) {
        this.code = code;
    }

    protected AbstractNodeWithCode(final AbstractNodeWithCode original) {
        this(original.code);
    }

    protected final boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        return context != null && context.hasModifiedSender();
    }

    protected final boolean isVirtualized(final VirtualFrame frame) {
        return getContext(frame) == null;
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return FrameAccess.getContext(frame, code);
    }

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        if (hasContextProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasContextProfile = ConditionProfile.createBinaryProfile();
            hasMarkerProfile = ConditionProfile.createBinaryProfile();
        }
        return FrameAccess.getContextOrMarker(frame, code, hasContextProfile, hasMarkerProfile);
    }
}
