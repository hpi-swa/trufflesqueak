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
        assert stackIndex >= 0;
        this.frameSlotWriteNode = FrameSlotWriteNode.create(code.getStackSlot(stackIndex));
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doStoreVirtualized(VirtualFrame frame, Object value) {
        frameSlotWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doStore(VirtualFrame frame, Object value) {
        getContext(frame).atTempPut(tempIndex, value);
    }
}
