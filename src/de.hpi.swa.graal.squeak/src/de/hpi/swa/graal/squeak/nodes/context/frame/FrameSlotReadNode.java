package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class FrameSlotReadNode extends AbstractFrameSlotNode {

    public static FrameSlotReadNode create(final CompiledCodeObject code, final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(code, frameSlot);
    }

    public static FrameSlotReadNode createForStackPointer(final CompiledCodeObject code) {
        return FrameSlotReadNodeGen.create(code, code.stackPointerSlot);
    }

    protected FrameSlotReadNode(final CompiledCodeObject code, final FrameSlot frameSlot) {
        super(code, frameSlot);
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(guards = "frame.isInt(slot)")
    protected final int readInt(final Frame frame) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    @Specialization(guards = "frame.isLong(slot)")
    protected final long readLong(final Frame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "frame.isDouble(slot)")
    protected final double readDouble(final Frame frame) {
        return FrameUtil.getDoubleSafe(frame, slot);
    }

    @Specialization(guards = "frame.isBoolean(slot)")
    protected final boolean readBool(final Frame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(guards = "frame.isObject(slot)")
    protected final Object readObject(final Frame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }

    @Fallback
    protected final Object doFail() {
        throw new SqueakException("Trying to read from illegal slot:", this);
    }

    protected final boolean isIllegal() {
        return frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal;
    }
}
