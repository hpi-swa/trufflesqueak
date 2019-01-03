package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReceiverAndArgumentsNode extends Node {
    public static ReceiverAndArgumentsNode create() {
        return new ReceiverAndArgumentsNode();
    }

    @ExplodeLoop
    public static Object[] executeGet(final VirtualFrame frame) {
        final Object[] frameArguments = frame.getArguments();
        final int rcvrAndArgsLength = frameArguments.length - FrameAccess.RECEIVER;
        final Object[] rcvrAndArgs = new Object[rcvrAndArgsLength];
        System.arraycopy(frameArguments, FrameAccess.RECEIVER, rcvrAndArgs, 0, rcvrAndArgsLength);
        return rcvrAndArgs;
    }
}
