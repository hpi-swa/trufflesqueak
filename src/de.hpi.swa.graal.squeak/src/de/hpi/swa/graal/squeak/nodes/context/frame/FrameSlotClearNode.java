package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public abstract class FrameSlotClearNode extends AbstractFrameSlotNode {

    protected FrameSlotClearNode(final FrameSlot slot) {
        super(slot);
    }

    public static FrameSlotClearNode create(final FrameSlot slot) {
        return FrameSlotClearNodeGen.create(slot);
    }

    public abstract void executeClear(Frame frame);

    @Specialization(guards = "isObject(frame)")
    protected final void clearObject(final Frame frame) {
        frame.setObject(frameSlot, null);
    }

    @Fallback
    protected static final void doNothing() {
        // Nothing to do.
    }

    protected final boolean isObject(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(frameSlot);
        return kind == FrameSlotKind.Object;
    }
}
