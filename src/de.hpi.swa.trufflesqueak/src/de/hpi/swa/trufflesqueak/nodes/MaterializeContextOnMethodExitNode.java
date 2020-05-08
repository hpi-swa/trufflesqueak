/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.GetContextNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.GetOrCreateContextNode;

public abstract class MaterializeContextOnMethodExitNode extends AbstractNode {
    public static MaterializeContextOnMethodExitNode create() {
        return MaterializeContextOnMethodExitNodeGen.create();
    }

    public abstract void execute(VirtualFrame frame);

    @Specialization(guards = {"image.lastSeenContext == null", "!getContextNode.hasContext(frame)", "getContextNode.execute(frame).hasEscaped()"}, limit = "1")
    protected static final void doStartMaterialization(final VirtualFrame frame,
                    @Shared("getContextNode") @Cached final GetContextNode getContextNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        image.lastSeenContext = getContextNode.execute(frame);
    }

    @Specialization(guards = {"image.lastSeenContext != null"})
    protected static final void doMaterialize(final VirtualFrame frame,
                    @Cached("createBinaryProfile()") final ConditionProfile isNotLastSeenContextProfile,
                    @Cached("createBinaryProfile()") final ConditionProfile continueProfile,
                    @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final ContextObject lastSeenContext = image.lastSeenContext;
        final ContextObject context = getOrCreateContextNode.executeGet(frame);
        if (isNotLastSeenContextProfile.profile(context != lastSeenContext)) {
            assert context.hasTruffleFrame();
            if (lastSeenContext != null && !lastSeenContext.hasMaterializedSender()) {
                lastSeenContext.setSender(context);
            }
            if (continueProfile.profile(!context.isTerminated() && context.hasEscaped())) {
                // Materialization needs to continue in parent frame.
                image.lastSeenContext = context;
            } else {
                // If context has not escaped, materialization can terminate here.
                image.lastSeenContext = null;
            }
        }
    }

    @Specialization(guards = {"getContextNode.hasContext(frame) || !getContextNode.execute(frame).hasEscaped()"}, limit = "1")
    protected final void doNothing(@SuppressWarnings("unused") final VirtualFrame frame,
                    @SuppressWarnings("unused") @Shared("getContextNode") @Cached final GetContextNode getContextNode) {
        /*
         * Nothing to do because neither was a child context materialized nor has this context been
         * requested and allocated.
         */
    }
}
