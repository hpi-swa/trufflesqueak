/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
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

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or marker, lazily initializes the latter if necessary. */
public final class GetContextOrMarkerNode extends AbstractNode {

    @CompilationFinal private FrameSlot contextSlot;
    @CompilationFinal private CompiledCodeObject methodOrBlock;
    @CompilationFinal private SqueakImageContext image;
    @CompilationFinal private FrameSlot markerSlot;
    private final ConditionProfile hasContextProfile = ConditionProfile.createBinaryProfile();

    public static GetContextOrMarkerNode create() {
        return new GetContextOrMarkerNode();
    }

    public Object execute(final VirtualFrame frame) {
        if (contextSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            contextSlot = FrameAccess.findContextSlot(frame);
            markerSlot = FrameAccess.findMarkerSlot(frame);
            image = lookupContext();
            methodOrBlock = FrameAccess.getMethodOrBlock(frame);
        }
        final ContextObject context = FrameAccess.getContext(frame, contextSlot);
        if (hasContextProfile.profile(context != null)) {
            return context;
        } else {
            return ContextObject.createLight(image, frame, methodOrBlock);
        }
    }

    public static Object getNotProfiled(final VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        final ContextObject context = FrameAccess.findContext(frame);
        if (context != null) {
            return context;
        } else {
            return ContextObject.createLight(SqueakLanguage.getContext(), frame, FrameAccess.getCodeObject(frame));
        }
    }
}
