package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class PopIntoTemporaryVariable extends SqueakBytecodeNode {
    @Child SqueakNode storeNode;

    final int tempIndex;

    public PopIntoTemporaryVariable(CompiledCodeObject method, int idx, int tempIdx) {
        super(method, idx);
        tempIndex = tempIdx;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        // TODO: figure out if we also need to handle remote temp vector stuff here
        storeNode = FrameSlotWriteNode.temp(method, tempIndex, stack.pop());
        statements.push(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeGeneric(frame);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
