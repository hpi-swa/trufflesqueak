package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

public abstract class FrameSlotReadNode extends AbstractFrameSlotReadNode {

    protected FrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final boolean readBoolean(final Frame frame) throws FrameSlotTypeException {
        return frame.getBoolean(frameSlot);
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

    @Specialization(replaces = {"readBoolean", "readLong", "readDouble", "readObject"})
    protected final Object readAny(final Frame frame) {
        final Object value = frame.getValue(frameSlot);
        assert value != null;
        return frame.getValue(frameSlot);
    }
}
