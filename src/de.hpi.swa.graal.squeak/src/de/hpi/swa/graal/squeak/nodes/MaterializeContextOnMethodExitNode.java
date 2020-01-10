/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class MaterializeContextOnMethodExitNode extends AbstractNodeWithCode {
    protected MaterializeContextOnMethodExitNode(final CompiledCodeObject code) {
        super(code);
    }

    public static MaterializeContextOnMethodExitNode create(final CompiledCodeObject code) {
        return MaterializeContextOnMethodExitNodeGen.create(code);
    }

    public abstract void execute(VirtualFrame frame);

    @Specialization(guards = {"!hasLastSeenContext(frame)", "!isVirtualized(frame)", "getContext(frame).hasEscaped()"})
    protected final void doStartMaterialization(final VirtualFrame frame) {
        code.image.lastSeenContext = getContext(frame);
    }

    @Specialization(guards = {"hasLastSeenContext(frame)"})
    protected final void doMaterialize(final VirtualFrame frame,
                    @Cached("createBinaryProfile()") final ConditionProfile isNotLastSeenContextProfile,
                    @Cached("createBinaryProfile()") final ConditionProfile continueProfile,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode) {
        final ContextObject lastSeenContext = code.image.lastSeenContext;
        final ContextObject context = getOrCreateContextNode.executeGet(frame, lastSeenContext.getProcess());
        if (isNotLastSeenContextProfile.profile(context != lastSeenContext)) {
            assert context.hasTruffleFrame();
            if (lastSeenContext != null && !lastSeenContext.hasMaterializedSender()) {
                FrameAccess.setSender(lastSeenContext.getOrCreateTruffleFrame(), context);
            }
            if (continueProfile.profile(!context.isTerminated() && context.hasEscaped())) {
                // Materialization needs to continue in parent frame.
                code.image.lastSeenContext = context;
            } else {
                // If context has not escaped, materialization can terminate here.
                code.image.lastSeenContext = null;
            }
        }
    }

    @Specialization(guards = {"isVirtualized(frame) || !getContext(frame).hasEscaped()"})
    protected final void doNothing(@SuppressWarnings("unused") final VirtualFrame frame) {
        /*
         * Nothing to do because neither was a child context materialized nor has this context been
         * requested and allocated.
         */
    }

    protected final boolean hasLastSeenContext(@SuppressWarnings("unused") final VirtualFrame frame) {
        return code.image.lastSeenContext != null;
    }
}
