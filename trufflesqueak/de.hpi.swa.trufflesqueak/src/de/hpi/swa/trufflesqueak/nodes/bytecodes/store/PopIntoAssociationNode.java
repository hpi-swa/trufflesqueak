package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoAssociationNode extends AbstractStoreIntoAssociationNode {
    @Child private PopStackNode popNode;

    public PopIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes, variableIndex);
        popNode = new PopStackNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, popNode.execute(frame));
    }

    @Override
    public String toString() {
        return "popIntoLit: " + variableIndex;
    }
}
