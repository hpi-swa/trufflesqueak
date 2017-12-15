package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoTempNode extends AbstractStoreIntoTempNode {
    @Child private PopStackNode popNode;

    public PopIntoTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes, tempIndex);
        popNode = new PopStackNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, popNode.execute(frame));
    }

    @Override
    public String toString() {
        return "popIntoTemp: " + tempIndex;
    }
}
