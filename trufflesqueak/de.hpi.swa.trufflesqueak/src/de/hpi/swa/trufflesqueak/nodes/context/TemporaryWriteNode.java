package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
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
        if (stackIndex >= 0) {
            frameSlotWriteNode = FrameSlotWriteNode.create(code.getStackSlot(stackIndex));
        }
    }

    @Specialization(guards = {"isVirtualized(frame, code)"})
    protected void doWriteVirtualized(VirtualFrame frame, Object value) {
        assert value != null;
        frameSlotWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame, code)"})
    protected void doWrite(VirtualFrame frame, Object value) {
        assert value != null;
        getContext(frame).atTempPut(tempIndex, value);
    }
}
