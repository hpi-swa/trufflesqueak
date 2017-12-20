package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoAssociationNode extends AbstractStoreIntoAssociationNode {
    @Child private PopStackNode popNode;

    public PopIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes, variableIndex);
    }

    @Override
    protected AbstractStackNode getValueNode() {
        return new PopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(frame);
    }

    @Override
    public String toString() {
        return "popIntoLit: " + variableIndex;
    }
}
