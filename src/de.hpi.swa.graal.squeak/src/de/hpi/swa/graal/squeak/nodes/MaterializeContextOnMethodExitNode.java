package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.MaterializeContextOnMethodExitNodeGen.SetSenderNodeGen;

public abstract class MaterializeContextOnMethodExitNode extends AbstractNodeWithCode {
    protected static ContextObject lastSeenContext;

    public static MaterializeContextOnMethodExitNode create(final CompiledCodeObject code) {
        return MaterializeContextOnMethodExitNodeGen.create(code);
    }

    protected MaterializeContextOnMethodExitNode(final CompiledCodeObject code) {
        super(code);
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
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create()") final SetSenderNode setSenderNode) {
        final ContextObject context = getOrCreateContextNode.executeGet(frame);
        if (context != lastSeenContext) {
            assert context.hasTruffleFrame();
            setSenderNode.execute(lastSeenContext, context);
            if (!context.isTerminated() && context.hasEscaped()) {
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

    protected abstract static class SetSenderNode extends Node {

        public static SetSenderNode create() {
            return SetSenderNodeGen.create();
        }

        protected abstract void execute(ContextObject childContext, ContextObject context);

        @Specialization(guards = {"childContext != null", "!childContext.hasMaterializedSender()"})
        protected static final void doSet(final ContextObject childContext, final ContextObject context) {
            childContext.setSender(context);
        }

        @Fallback
        @SuppressWarnings("unused")
        protected final void doNothing(final ContextObject childContext, final ContextObject context) {
            // Sender does not need to be set.
        }
    }
}
