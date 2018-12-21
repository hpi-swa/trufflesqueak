package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class GetBlockFrameArgumentsNode extends Node {

    public static GetBlockFrameArgumentsNode create() {
        return GetBlockFrameArgumentsNodeGen.create();
    }

    public abstract Object[] execute(BlockClosureObject block, Object senderOrMarker, Object[] objects);

    @Specialization(guards = {"objects.length == numObjects", "block.getStack().length == numCopied", "block.getCallTarget() == cachedCallTarget"}, limit = "3", assumptions = "callTargetStable")
    @ExplodeLoop
    protected static final Object[] doCached(final BlockClosureObject block, final Object senderOrMarker, final Object[] objects,
                    @SuppressWarnings("unused") @Cached("block.getCallTarget()") final RootCallTarget cachedCallTarget,
                    @SuppressWarnings("unused") @Cached("block.getCallTargetStable()") final Assumption callTargetStable,
                    @Cached("block.getStack().length") final int numCopied,
                    @Cached("objects.length") final int numObjects) {
        final Object[] copied = block.getStack();
        final Object[] arguments = fillInSpecial(block, senderOrMarker, numObjects, numCopied);
        for (int i = 0; i < numObjects + numCopied; i++) {
            if (i < numObjects) {
                arguments[FrameAccess.ARGUMENTS_START + i] = objects[i];
            } else {
                arguments[FrameAccess.ARGUMENTS_START + i] = copied[i - numObjects];
            }
        }
        return arguments;
    }

    @Specialization(replaces = "doCached")
    protected static final Object[] doUncached(final BlockClosureObject block, final Object senderOrMarker, final Object[] objects) {
        final int numObjects = objects.length;
        final Object[] copied = block.getStack();
        final int numCopied = copied.length;
        final Object[] arguments = fillInSpecial(block, senderOrMarker, numObjects, numCopied);
        for (int i = 0; i < numObjects; i++) {
            arguments[FrameAccess.ARGUMENTS_START + i] = objects[i];
        }
        for (int i = 0; i < numCopied; i++) {
            arguments[FrameAccess.ARGUMENTS_START + numObjects + i] = copied[i];
        }
        return arguments;
    }

    private static Object[] fillInSpecial(final BlockClosureObject block, final Object senderOrMarker, final int numObjects, final int numCopied) {
        final CompiledBlockObject blockObject = block.getCompiledBlock();
        assert blockObject.getNumArgs() == numObjects : "number of required and provided block arguments do not match";
        final Object[] arguments = new Object[FrameAccess.ARGUMENTS_START +
                        numObjects +
                        numCopied];
        arguments[FrameAccess.METHOD] = blockObject;
        // Sender is thisContext (or marker)
        arguments[FrameAccess.SENDER_OR_SENDER_MARKER] = senderOrMarker;
        arguments[FrameAccess.CLOSURE_OR_NULL] = block;
        arguments[FrameAccess.RECEIVER] = block.getReceiver();
        return arguments;
    }
}
