package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PopIntoReceiverVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode storeNode;

    final int receiverIndex;

    public PopIntoReceiverVariableNode(CompiledCodeObject method, int idx, int receiverIdx) {
        super(method, idx);
        receiverIndex = receiverIdx;
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
