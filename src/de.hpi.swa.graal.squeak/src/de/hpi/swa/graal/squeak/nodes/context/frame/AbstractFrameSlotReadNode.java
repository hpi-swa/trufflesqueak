package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;

@ImportStatic(FrameSlotKind.class)
public abstract class AbstractFrameSlotReadNode extends AbstractFrameSlotNode {
    protected AbstractFrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(guards = "frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == Boolean")
    protected final boolean readBoolean(final Frame frame) {
        return FrameUtil.getBooleanSafe(frame, frameSlot);
    }

    @Specialization(guards = "frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == Long")
    protected final long readLong(final Frame frame) {
        return FrameUtil.getLongSafe(frame, frameSlot);
    }

    @Specialization(guards = "frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == Double")
    protected final double readDouble(final Frame frame) {
        return FrameUtil.getDoubleSafe(frame, frameSlot);
    }
}
