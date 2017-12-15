package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class StoreIntoAssociationNode extends SqueakBytecodeNode {
    @Child WriteNode storeNode;
    public static final int ASSOCIATION_VALUE = 1;
    protected final int variableIndex;

    public StoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes);
        this.variableIndex = variableIndex;
        storeNode = ObjectAtPutNode.create(ASSOCIATION_VALUE, new MethodLiteralNode(code, variableIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, top(frame));
    }

    @Override
    public String toString() {
        return "storeIntoLit: " + variableIndex;
    }
}