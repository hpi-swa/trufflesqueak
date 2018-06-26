package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

public abstract class TemporaryWriteNode extends AbstractWriteNode {
    @Child private FrameSlotWriteNode frameSlotWriteNode;
    private final long tempIndex;

    public static TemporaryWriteNode create(final CompiledCodeObject code, final long tempIndex) {
        return TemporaryWriteNodeGen.create(code, tempIndex);
    }

    protected TemporaryWriteNode(final CompiledCodeObject code, final long tempIndex) {
        this.tempIndex = tempIndex;
        // Perform checks to ensure a correct FrameSlotWriteNode is created, otherwise fail which
        // happens
        // when the decoder is decoding garbage.
        if (0 <= tempIndex && tempIndex <= CONTEXT.MAX_STACK_SIZE && code.canBeVirtualized()) {
            final FrameSlot stackSlot = code.getStackSlot((int) tempIndex);
            if (stackSlot != null) {
                frameSlotWriteNode = FrameSlotWriteNode.create(stackSlot);
            }
        }
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doWriteVirtualized(final VirtualFrame frame, final Object value) {
        assert value != null;
        frameSlotWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected final void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).atTempPut(tempIndex, value);
    }
}
