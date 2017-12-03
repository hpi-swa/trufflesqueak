package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class StoreAndPopTemporaryVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode storeNode;

    final int tempIndex;

    public StoreAndPopTemporaryVariableNode(CompiledCodeObject code, int index, int tempIdx) {
        super(code, index);
        tempIndex = tempIdx;
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
