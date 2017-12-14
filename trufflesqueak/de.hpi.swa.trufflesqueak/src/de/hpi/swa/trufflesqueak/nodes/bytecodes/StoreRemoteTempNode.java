package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class StoreRemoteTempNode extends RemoteTempBytecodeNode {
    @Child ObjectAtPutNode writeTempNode;

    public StoreRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
        writeTempNode = ObjectAtPutNode.create(indexInArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return writeTempNode.executeWrite(getTempArray(frame), top(frame));
    }

    @Override
    public String toString() {
        return String.format("storeIntoTemp: %d inVectorAt: %d", indexInArray, indexOfArray);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
