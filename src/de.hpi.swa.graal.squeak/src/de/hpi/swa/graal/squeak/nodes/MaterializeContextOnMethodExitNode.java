package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

public abstract class MaterializeContextOnMethodExitNode extends AbstractNodeWithCode {
    private static ContextObject lastSeenContext;

    protected MaterializeContextOnMethodExitNode(final CompiledCodeObject code) {
        super(code);
    }

    public static MaterializeContextOnMethodExitNode create(final CompiledCodeObject code) {
        return MaterializeContextOnMethodExitNodeGen.create(code);
    }

    public abstract void execute(VirtualFrame frame);

    public static final void reset() {
        lastSeenContext = null;
    }

    public static final void stopMaterializationHere() {
        if (lastSeenContext != null) {
            reset();
        }
    }

    @Specialization(guards = {"!hasLastSeenContext(frame)", "!isVirtualized(frame)", "getContext(frame).hasEscaped()"})
    protected final void doStartMaterialization(final VirtualFrame frame) {
        lastSeenContext = getContext(frame);
    }

    @Specialization(guards = {"hasLastSeenContext(frame)"})
    protected static final void doMaterialize(final VirtualFrame frame,
                    @Cached("createCountingProfile()") final ConditionProfile isNotLastSeenContextProfile,
                    @Cached("createCountingProfile()") final ConditionProfile lastSeenNeedsSenderProfile,
                    @Cached("createCountingProfile()") final ConditionProfile continueProfile,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode) {
        final ContextObject context = getOrCreateContextNode.executeGet(frame);
        if (isNotLastSeenContextProfile.profile(context != lastSeenContext)) {
            assert context.hasTruffleFrame();
            if (lastSeenNeedsSenderProfile.profile(lastSeenContext != null && !lastSeenContext.hasMaterializedSender())) {
                lastSeenContext.setSender(context);
            }
            if (continueProfile.profile(!context.isTerminated() && context.hasEscaped())) {
                // Materialization needs to continue in parent frame.
                lastSeenContext = context;
            } else {
                // If context has not escaped, materialization can terminate here.
                lastSeenContext = null;
            }
        }
    }

    @Fallback
    protected final void doNothing() {
        /*
         * Nothing to do because neither was a child context materialized nor has this context been
         * requested and allocated.
         */
    }

    protected static final boolean hasLastSeenContext(@SuppressWarnings("unused") final VirtualFrame frame) {
        return lastSeenContext != null;
    }
}
