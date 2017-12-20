package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameTemporaryNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractStackNode;

public abstract class AbstractStoreIntoRemoteTempNode extends AbstractBytecodeNode {
    @Child protected ObjectAtPutNode storeNode;
    @CompilationFinal private final int indexInArray;
    @CompilationFinal private final int indexOfArray;

    public AbstractStoreIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes);
        this.indexInArray = indexInArray;
        this.indexOfArray = indexOfArray;
        storeNode = ObjectAtPutNode.create(indexInArray, new FrameTemporaryNode(code, indexOfArray), getValueNode());
    }

    protected abstract AbstractStackNode getValueNode();

    @Override
    public String toString() {
        return String.format("%sIntoTemp: %d inVectorAt: %d", getTypeName(), this.indexInArray, this.indexOfArray);
    }

    protected abstract String getTypeName();
}
