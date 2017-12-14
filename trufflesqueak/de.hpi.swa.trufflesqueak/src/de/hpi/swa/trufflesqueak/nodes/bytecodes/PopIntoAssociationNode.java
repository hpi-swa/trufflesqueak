package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PopIntoAssociationNode extends StoreIntoAssociationNode {

    PopIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes, variableIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, pop(frame));
    }

    @Override
    public String toString() {
        return "popIntoLit: " + variableIndex;
    }
}
