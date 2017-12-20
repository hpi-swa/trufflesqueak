package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractStackNode;

public abstract class AbstractStoreIntoReceiverVariableNode extends AbstractBytecodeNode {
    @Child ObjectAtPutNode storeNode;
    protected final int receiverIndex;

    public AbstractStoreIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes);
        this.receiverIndex = receiverIndex;
        storeNode = ObjectAtPutNode.create(receiverIndex, new FrameReceiverNode(code), getValueNode());
    }

    protected abstract AbstractStackNode getValueNode();
}
