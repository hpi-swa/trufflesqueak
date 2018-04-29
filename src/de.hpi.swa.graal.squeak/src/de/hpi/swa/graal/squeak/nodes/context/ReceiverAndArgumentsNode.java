package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ReceiverAndArgumentsNode extends SqueakNodeWithCode {
    public static ReceiverAndArgumentsNode create(final CompiledCodeObject code) {
        return ReceiverAndArgumentsNodeGen.create(code);
    }

    protected ReceiverAndArgumentsNode(final CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object[] doRcvrAndArgsVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        final Object[] frameArguments = frame.getArguments();
        final Object[] rcvrAndArgs = new Object[frameArguments.length - FrameAccess.RECEIVER];
        for (int i = 0; i < rcvrAndArgs.length; i++) {
            rcvrAndArgs[i] = frameArguments[FrameAccess.RECEIVER + i];
        }
        return rcvrAndArgs;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object[] doRcvrAndArgs(final VirtualFrame frame) {
        return getContext(frame).getReceiverAndArguments();
    }
}
