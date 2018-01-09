package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.FrameAccess;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public abstract class ReceiverNode extends SqueakNodeWithCode {
    public static ReceiverNode create(CompiledCodeObject code) {
        return ReceiverNodeGen.create(code);
    }

    protected ReceiverNode(CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doReceiverVirtualized(VirtualFrame frame) {
        return frame.getArguments()[FrameAccess.RECEIVER];
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doReceiver(VirtualFrame frame) {
        return getContext(frame).getReceiver();
    }
}
