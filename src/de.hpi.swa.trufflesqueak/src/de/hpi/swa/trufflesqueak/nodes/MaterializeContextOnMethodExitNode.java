/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@SuppressWarnings("truffle-inlining")
public abstract class MaterializeContextOnMethodExitNode extends AbstractNode {
    public static MaterializeContextOnMethodExitNode create() {
        return MaterializeContextOnMethodExitNodeGen.create();
    }

    public abstract void execute(VirtualFrame frame);

    @Specialization(guards = {"getSqueakImageContext(frame).lastSeenContext == null", "hasEscapedContext(frame)"})
    protected final void doStartMaterialization(final VirtualFrame frame) {
        getContext().lastSeenContext = FrameAccess.getContext(frame);
    }

    @Specialization(guards = {"getSqueakImageContext(frame).lastSeenContext != null"})
    protected final void doMaterialize(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached final InlinedConditionProfile isNotLastSeenContextProfile,
                    @Cached final InlinedConditionProfile continueProfile,
                    @Cached(inline = true) final GetOrCreateContextNode getOrCreateContextNode) {
        final SqueakImageContext image = getContext();
        final ContextObject lastSeenContext = image.lastSeenContext;
        final ContextObject context = getOrCreateContextNode.executeGet(frame, node);
        if (isNotLastSeenContextProfile.profile(node, context != lastSeenContext)) {
            assert context.hasTruffleFrame();
            if (lastSeenContext != null && !lastSeenContext.hasMaterializedSender()) {
                lastSeenContext.setSender(context);
            }
            if (continueProfile.profile(node, context.canBeReturnedTo() && context.hasEscaped())) {
                // Materialization needs to continue in parent frame.
                image.lastSeenContext = context;
            } else {
                // If context has not escaped, materialization can terminate here.
                image.lastSeenContext = null;
            }
        }
    }

    @Specialization(guards = {"!hasEscapedContext(frame)"})
    protected final void doNothing(@SuppressWarnings("unused") final VirtualFrame frame) {
        /*
         * Nothing to do because neither was a child context materialized nor has this context been
         * requested and allocated.
         */
    }

    /* Avoid that the DSL generates an assertion for this. */
    protected final SqueakImageContext getSqueakImageContext(@SuppressWarnings("unused") final VirtualFrame frame) {
        return getContext();
    }
}
