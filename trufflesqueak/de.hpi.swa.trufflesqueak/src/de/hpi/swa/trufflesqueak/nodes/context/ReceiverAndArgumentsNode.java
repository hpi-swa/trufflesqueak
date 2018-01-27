package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class ReceiverAndArgumentsNode extends SqueakNodeWithCode {
    public static ReceiverAndArgumentsNode create(CompiledCodeObject code) {
        return ReceiverAndArgumentsNodeGen.create(code);
    }

    protected ReceiverAndArgumentsNode(CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object[] doRcvrAndArgsVirtualized(VirtualFrame frame) {
        Object[] frameArguments = frame.getArguments();
        Object[] rcvrAndArgs = new Object[frameArguments.length - FrameAccess.RCVR_AND_ARGS_START];
        for (int i = 0; i < rcvrAndArgs.length; i++) {
            rcvrAndArgs[i] = frameArguments[FrameAccess.RCVR_AND_ARGS_START + i];
        }
        return rcvrAndArgs;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object[] doRcvrAndArgs(VirtualFrame frame) {
        return getContext(frame).getReceiverAndArguments();
    }
}
