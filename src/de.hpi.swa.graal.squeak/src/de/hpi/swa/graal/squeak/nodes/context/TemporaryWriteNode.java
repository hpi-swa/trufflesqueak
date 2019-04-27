package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

public abstract class TemporaryWriteNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode writeNode;

    protected TemporaryWriteNode(final CompiledCodeObject code, final int tempIndex) {
        super(code);
        writeNode = FrameSlotWriteNode.create(code.getStackSlot(tempIndex));
    }

    public static TemporaryWriteNode create(final CompiledCodeObject code, final int tempIndex) {
        return TemporaryWriteNodeGen.create(code, tempIndex);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization
    protected final void doWriteContext(final VirtualFrame frame, final ContextObject value) {
        assert value != null;
        value.markEscaped();
        writeNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isContextObject(value)"})
    protected final void doWriteOther(final VirtualFrame frame, final Object value) {
        assert value != null;
        writeNode.executeWrite(frame, value);
    }
}
