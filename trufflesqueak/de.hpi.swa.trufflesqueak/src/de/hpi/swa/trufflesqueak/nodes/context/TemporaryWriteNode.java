package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.helpers.AbstractWriteNode;

public abstract class TemporaryWriteNode extends AbstractWriteNode {
    @Child private FrameSlotWriteNode frameSlotWriteNode;
    @CompilationFinal private final long tempIndex;

    public static TemporaryWriteNode create(final CompiledCodeObject code, final long tempIndex) {
        return TemporaryWriteNodeGen.create(code, tempIndex);
    }

    public TemporaryWriteNode(final CompiledCodeObject code, final long tempIndex) {
        super(code);
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
    protected void doWriteVirtualized(final VirtualFrame frame, final Object value) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        assert value != null;
        frameSlotWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).atTempPut(tempIndex, value);
    }
}
