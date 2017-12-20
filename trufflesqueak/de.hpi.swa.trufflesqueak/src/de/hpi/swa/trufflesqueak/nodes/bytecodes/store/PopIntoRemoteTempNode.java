package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

    public PopIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
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
    protected String getTypeName() {
        return "pop";
    }
}
