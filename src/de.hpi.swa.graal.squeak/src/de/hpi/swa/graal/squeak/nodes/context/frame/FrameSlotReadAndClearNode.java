package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

public abstract class FrameSlotReadAndClearNode extends AbstractFrameSlotNode {
    protected final boolean clear;

    protected FrameSlotReadAndClearNode(final FrameSlot slot, final boolean clear) {
        super(slot);
        this.clear = clear;
    }

    public static FrameSlotReadAndClearNode create(final FrameSlot slot, final boolean clear) {
        return FrameSlotReadAndClearNodeGen.create(slot, clear);
    }

    public abstract Object executeReadAndClear(Frame frame);

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

    @Specialization(guards = "clear", rewriteOn = FrameSlotTypeException.class)
    protected final Object readObjectClear(final Frame frame) throws FrameSlotTypeException {
        final Object value = frame.getObject(frameSlot);
        frame.setObject(frameSlot, null);
        return value;
    }

    @Specialization(guards = "!clear", rewriteOn = FrameSlotTypeException.class)
    protected final Object readObject(final Frame frame) throws FrameSlotTypeException {
        return frame.getObject(frameSlot);
    }

    @Specialization(guards = "clear", replaces = {"readBoolean", "readLong", "readDouble", "readObjectClear"})
    protected final Object readAnyClear(final Frame frame) {
        final Object value = frame.getValue(frameSlot);
        assert value != null;
        frame.setObject(frameSlot, null);
        return value;
    }

    @Fallback
    protected final Object readAny(final Frame frame) {
        final Object value = frame.getValue(frameSlot);
        assert value != null;
        return value;
    }
}
