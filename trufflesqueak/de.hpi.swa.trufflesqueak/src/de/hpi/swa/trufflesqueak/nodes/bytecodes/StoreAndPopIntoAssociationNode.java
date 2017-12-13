package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class StoreAndPopIntoAssociationNode extends StoreIntoAssociationNode {

    StoreAndPopIntoAssociationNode(CompiledCodeObject code, int index, int variableIndex) {
        super(code, index, variableIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return node.executeWrite(frame, pop(frame));
    }

}
