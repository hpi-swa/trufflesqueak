package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public abstract class AbstractStoreIntoAssociationNode extends AbstractBytecodeNode {
    @Child WriteNode storeNode;
    public static final int ASSOCIATION_VALUE = 1;
    protected final int variableIndex;

    public AbstractStoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes);
        this.variableIndex = variableIndex;
        storeNode = ObjectAtPutNode.create(ASSOCIATION_VALUE, new MethodLiteralNode(code, variableIndex));
    }

}