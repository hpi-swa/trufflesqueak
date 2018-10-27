package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

public abstract class TemporaryWriteNode extends AbstractNodeWithCode {
    protected final int tempIndex;

    public static TemporaryWriteNode create(final CompiledCodeObject code, final int tempIndex) {
        return TemporaryWriteNodeGen.create(code, tempIndex);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    protected TemporaryWriteNode(final CompiledCodeObject code, final int tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doWriteVirtualized(final VirtualFrame frame, final Object value,
                    @Cached("create(code.getStackSlot(tempIndex))") final FrameSlotWriteNode writeNode) {
        assert value != null;
        assert 0 <= tempIndex && tempIndex <= CONTEXT.MAX_STACK_SIZE;
        writeNode.executeWrite(frame, value);
    }

    @Fallback
    protected final void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).atTempPut(tempIndex, value);
    }
}
