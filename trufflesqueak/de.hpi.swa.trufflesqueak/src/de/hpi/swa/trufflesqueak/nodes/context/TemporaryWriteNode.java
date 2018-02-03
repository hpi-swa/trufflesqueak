package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractWriteNode;

public abstract class TemporaryWriteNode extends AbstractWriteNode {
    @Child private FrameSlotWriteNode frameSlotWriteNode;
    @CompilationFinal private final int tempIndex;

    public static TemporaryWriteNode create(CompiledCodeObject code, int tempIndex) {
        return TemporaryWriteNodeGen.create(code, tempIndex);
    }

    public TemporaryWriteNode(CompiledCodeObject code, int tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
        int stackIndex = code.convertTempIndexToStackIndex(tempIndex);
        // Perform checks to ensure a correct FrameSlotWriteNode is created, otherwise fail which happens
        // when the decoder is decoding garbage.
        if (0 <= stackIndex && stackIndex <= CONTEXT.MAX_STACK_SIZE) {
            FrameSlot stackSlot = code.getStackSlot(stackIndex);
            if (stackSlot != null) {
                frameSlotWriteNode = FrameSlotWriteNode.create(stackSlot);
            }
        }
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doWriteVirtualized(VirtualFrame frame, Object value) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        assert value != null;
        frameSlotWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doWrite(VirtualFrame frame, Object value) {
        assert value != null;
        getContext(frame).atTempPut(tempIndex, value);
    }
}
