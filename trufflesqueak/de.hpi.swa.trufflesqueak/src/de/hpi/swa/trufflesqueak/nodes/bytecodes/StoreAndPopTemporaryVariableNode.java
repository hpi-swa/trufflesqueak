package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class StoreAndPopTemporaryVariableNode extends SqueakBytecodeNode {
    @Child FrameSlotWriteNode tempNode;

    public StoreAndPopTemporaryVariableNode(CompiledCodeObject code, int index, int tempIndex) {
        super(code, index);
        if (code.getNumStackSlots() <= tempIndex) {
            // sometimes we'll decode more bytecodes than we have slots ... that's fine
        } else {
            tempNode = FrameSlotWriteNode.create(code.getStackSlot(tempIndex));
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return tempNode.executeWrite(frame, pop(frame));
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
