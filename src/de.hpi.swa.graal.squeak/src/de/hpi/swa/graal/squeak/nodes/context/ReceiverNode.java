package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ReceiverNode extends SqueakNodeWithCode {
    protected ReceiverNode(final CompiledCodeObject code) {
        super(code);
    }

    public static ReceiverNode create(final CompiledCodeObject code) {
        return ReceiverNodeGen.create(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected static final Object doReceiverVirtualized(final VirtualFrame frame) {
        return FrameAccess.getReceiver(frame);
    }

    @Fallback
    protected final Object doReceiver(final VirtualFrame frame) {
        return getContext(frame).getReceiver();
    }
}
