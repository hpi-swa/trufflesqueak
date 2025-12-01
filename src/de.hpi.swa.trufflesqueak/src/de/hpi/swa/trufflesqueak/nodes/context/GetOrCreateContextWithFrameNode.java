/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;

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
        return doGetOrCreate(frame, null, InlinedCountingConditionProfile.getUncached());
    }

    public abstract ContextObject executeGet(VirtualFrame frame, Node node);

    public final ContextObject executeGet(final VirtualFrame frame) {
        return executeGet(frame, this);
    }

    @Specialization
    protected static final ContextObject doGetOrCreate(final VirtualFrame frame, final Node node,
                    @Cached final InlinedCountingConditionProfile hasContextProfile) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (context != null) {
            if (hasContextProfile.profile(node, !context.hasTruffleFrame())) {
                context.setTruffleFrame(frame.materialize());
            }
            return context;
        } else {
            return new ContextObject(getContext(node), frame.materialize());
        }
    }
}
