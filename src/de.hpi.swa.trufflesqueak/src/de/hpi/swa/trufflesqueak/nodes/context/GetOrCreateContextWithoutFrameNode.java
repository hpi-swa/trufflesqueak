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
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or lazily initializes one if necessary. */
@GenerateInline
@GenerateCached(true)
public abstract class GetOrCreateContextWithoutFrameNode extends AbstractNode {
    @NeverDefault
    public static GetOrCreateContextWithoutFrameNode create() {
        return GetOrCreateContextWithoutFrameNodeGen.create();
    }

    public abstract ContextObject execute(VirtualFrame frame, Node node);

    public final ContextObject execute(final VirtualFrame frame) {
        return execute(frame, this);
    }

    @Specialization
    public static ContextObject getContext(final VirtualFrame frame, final Node node,
                    @Cached final InlinedConditionProfile hasContextProfile) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (hasContextProfile.profile(node, context != null)) {
            return context;
        } else {
            return new ContextObject(frame);
        }
    }
}
