package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class CreateArgumentsNode extends Node {
    public static CreateArgumentsNode create() {
        return CreateArgumentsNodeGen.create();
    }

    public abstract Object[] executeCreate(CompiledCodeObject method, Object contextOrMarker, Object[] receiverAndArguments);

    @Specialization(guards = {"rcvrAndArgs.length == cachedLen"}, limit = "3")
    @ExplodeLoop
    protected static final Object[] cached(final CompiledCodeObject code, final Object sender, final Object[] rcvrAndArgs,
                    @Cached("rcvrAndArgs.length") final int cachedLen) {
        final Object[] arguments = new Object[FrameAccess.RECEIVER + cachedLen];
        arguments[FrameAccess.METHOD] = code;
        arguments[FrameAccess.SENDER_OR_SENDER_MARKER] = sender;
        arguments[FrameAccess.CLOSURE_OR_NULL] = null;
        for (int i = 0; i < cachedLen; i++) {
            arguments[FrameAccess.RECEIVER + i] = rcvrAndArgs[i];
        }
        return arguments;
    }

    /*
     * Similar method body to `cached`, except @ExplodeLoop and `cachedLen` (do not extract method,
     * code copied on purpose).
     */
    @Specialization(replaces = "cached")
    protected static final Object[] uncached(final CompiledCodeObject code, final Object sender, final Object[] rcvrAndArgs) {
        final Object[] arguments = new Object[FrameAccess.RECEIVER + rcvrAndArgs.length];
        arguments[FrameAccess.METHOD] = code;
        arguments[FrameAccess.SENDER_OR_SENDER_MARKER] = sender;
        arguments[FrameAccess.CLOSURE_OR_NULL] = null;
        for (int i = 0; i < rcvrAndArgs.length; i++) {
            arguments[FrameAccess.RECEIVER + i] = rcvrAndArgs[i];
        }
        return arguments;
    }
}
