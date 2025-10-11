/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or lazily initializes one if necessary. */
public final class GetContextOrMarkerNode extends AbstractNode {

    @NeverDefault
    public static GetContextOrMarkerNode create() {
        return new GetContextOrMarkerNode();
    }

    public ContextObject execute(final VirtualFrame frame) {
        return getContext(SqueakImageContext.get(this), frame);
    }

    public static ContextObject getNotProfiled(final VirtualFrame frame) {
        return getContext(SqueakImageContext.getSlow(), frame);
    }

    public static ContextObject getContext(final SqueakImageContext image, final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (context != null) {
            return context;
        } else {
            return ContextObject.create(image, frame);
        }
    }
}
