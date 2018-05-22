package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class FrameSlotReadNode extends AbstractFrameSlotNode {

    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    public static FrameSlotReadNode createForContextOrMarker() {
        return FrameSlotReadNodeGen.create(CompiledCodeObject.thisContextOrMarkerSlot);
    }

    public static FrameSlotReadNode createForInstructionPointer() {
        return FrameSlotReadNodeGen.create(CompiledCodeObject.instructionPointerSlot);
    }

    public static FrameSlotReadNode createForStackPointer() {
        return FrameSlotReadNodeGen.create(CompiledCodeObject.stackPointerSlot);
    }

    protected FrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(guards = "isInt(frame)")
    protected final int readInt(final VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    @Specialization(guards = "isLong(frame)")
    protected final long readLong(final VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "isDouble(frame)")
    protected final double readDouble(final VirtualFrame frame) {
        return FrameUtil.getDoubleSafe(frame, slot);
    }

    @Specialization(guards = "isBoolean(frame)")
    protected final boolean readBool(final VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(guards = "isObject(frame)")
    protected final Object readObject(final VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }

    @Specialization(guards = "isIllegal(frame)")
    protected static final Object readIllegal(@SuppressWarnings("unused") final VirtualFrame frame) {
        throw new SqueakException("Trying to read from illegal slot");
    }

    protected final boolean isInt(final VirtualFrame frame) {
        return frame.isInt(slot);
    }

    protected final boolean isLong(final VirtualFrame frame) {
        return frame.isLong(slot);
    }

    protected final boolean isDouble(final VirtualFrame frame) {
        return frame.isDouble(slot);
    }

    protected final boolean isBoolean(final VirtualFrame frame) {
        return frame.isBoolean(slot);
    }

    protected final boolean isObject(final VirtualFrame frame) {
        return frame.isObject(slot);
    }

    protected final boolean isIllegal(@SuppressWarnings("unused") final VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Illegal;
    }
}
