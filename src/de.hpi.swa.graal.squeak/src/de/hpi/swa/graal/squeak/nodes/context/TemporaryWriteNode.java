package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

public abstract class TemporaryWriteNode extends AbstractNodeWithCode {
    private final long tempIndex;
    @Child private FrameSlotWriteNode frameSlotWriteNode;

    public static TemporaryWriteNode create(final CompiledCodeObject code, final long tempIndex) {
        return TemporaryWriteNodeGen.create(code, tempIndex);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    protected TemporaryWriteNode(final CompiledCodeObject code, final long tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doWriteVirtualized(final VirtualFrame frame, final Object value) {
        assert value != null;
        getFrameSlotWriteNode().executeWrite(frame, value);
    }

    @Fallback
    protected final void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).atTempPut(tempIndex, value);
    }

    private FrameSlotWriteNode getFrameSlotWriteNode() {
        if (frameSlotWriteNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert 0 <= tempIndex && tempIndex <= CONTEXT.MAX_STACK_SIZE;
            final FrameSlot stackSlot = code.getStackSlot((int) tempIndex);
            frameSlotWriteNode = insert(FrameSlotWriteNode.create(stackSlot));
        }
        return frameSlotWriteNode;
    }
}
