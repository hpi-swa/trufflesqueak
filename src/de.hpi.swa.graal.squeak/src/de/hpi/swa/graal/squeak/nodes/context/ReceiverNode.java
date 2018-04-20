package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ReceiverNode extends SqueakNodeWithCode {
    public static ReceiverNode create(final CompiledCodeObject code) {
        return ReceiverNodeGen.create(code);
    }

    protected ReceiverNode(final CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doReceiverVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return frame.getArguments()[FrameAccess.RECEIVER];
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doReceiver(final VirtualFrame frame) {
        return getContext(frame).getReceiver();
    }
}
