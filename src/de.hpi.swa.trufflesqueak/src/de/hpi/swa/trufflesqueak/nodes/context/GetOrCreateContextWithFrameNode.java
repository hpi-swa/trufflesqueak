/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@GenerateInline
@GenerateCached(true)
public abstract class GetOrCreateContextWithFrameNode extends AbstractNode {

    @NeverDefault
    public static GetOrCreateContextWithFrameNode create() {
        return GetOrCreateContextWithFrameNodeGen.create();
    }

    public static final ContextObject executeUncached(final VirtualFrame frame) {
        return doGetOrCreate(frame);
    }

    public abstract ContextObject executeGet(VirtualFrame frame, Node node);

    public final ContextObject executeGet(final VirtualFrame frame) {
        return executeGet(frame, this);
    }

    /* No profiles, see GetOrCreateContextWithoutFrameNode#getContext. */
    @Specialization
    protected static final ContextObject doGetOrCreate(final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (context == null) {
            return ContextObject.createMaterialized(frame.materialize());
        }
        if (!context.hasTruffleFrame()) {
            context.setTruffleFrame(frame.materialize());
        }
        return context;
    }
}
