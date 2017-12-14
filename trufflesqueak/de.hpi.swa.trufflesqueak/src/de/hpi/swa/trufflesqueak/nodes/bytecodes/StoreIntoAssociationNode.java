package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class StoreIntoAssociationNode extends SqueakBytecodeNode {
    @Child WriteNode node;

    StoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes);
        node = ObjectAtPutNode.create(1, new MethodLiteralNode(code, variableIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return node.executeWrite(frame, top(frame));
    }
}