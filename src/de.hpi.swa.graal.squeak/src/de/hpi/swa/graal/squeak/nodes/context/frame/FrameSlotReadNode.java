package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class FrameSlotReadNode extends AbstractFrameSlotNode {

    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    public static FrameSlotReadNode createForStackPointer(final CompiledCodeObject code) {
        return FrameSlotReadNodeGen.create(code.stackPointerSlot);
    }

    protected FrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final boolean readBoolean(final Frame frame) throws FrameSlotTypeException {
        return frame.getBoolean(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final int readInt(final Frame frame) throws FrameSlotTypeException {
        return frame.getInt(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final long readLong(final Frame frame) throws FrameSlotTypeException {
        return frame.getLong(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final double readDouble(final Frame frame) throws FrameSlotTypeException {
        return frame.getDouble(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final Object readObject(final Frame frame) throws FrameSlotTypeException {
        return frame.getObject(frameSlot);
    }

    @Specialization(replaces = {"readBoolean", "readInt", "readLong", "readDouble", "readObject"})
    protected final Object readAny(final Frame frame) {
        return frame.getValue(frameSlot);
    }
}
