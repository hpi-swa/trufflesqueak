package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.MaterializeContextOnMethodExitNodeGen.SetSenderNodeGen;

public abstract class MaterializeContextOnMethodExitNode extends AbstractNode {
    @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();
    @Child private SetSenderNode setSenderNode = SetSenderNode.create();

    protected static ContextObject lastSeenContext;

    public static MaterializeContextOnMethodExitNode create() {
        return MaterializeContextOnMethodExitNodeGen.create();
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

    @Specialization(guards = {"lastSeenContext != null || !isFullyVirtualized(frame)"})
    protected final void doMaterialize(final VirtualFrame frame) {
        final ContextObject context = getOrCreateContextNode.executeGet(frame);
        if (context != lastSeenContext) {
            setSenderNode.execute(lastSeenContext, context);
            if (context.hasEscaped()) {
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

    protected abstract static class SetSenderNode extends AbstractNode {

        protected static SetSenderNode create() {
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
