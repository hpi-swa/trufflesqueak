package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class StoreIntoRemoteTemporaryLocationNode extends AbstractRemoteTemporaryLocationNode {
    @Child private ObjectAtPutNode storeNode;

    public StoreIntoRemoteTemporaryLocationNode(CompiledCodeObject code, int indexInArray, int indexOfArray) {
        super(code, indexInArray, indexOfArray);
        storeNode = ObjectAtPutNode.create(indexInArray);
    }

    public void executeWrite(VirtualFrame frame, Object value) {
        storeNode.executeWrite(getTempArrayNode.executeRead(frame), value);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Should not be executed like this");
    }
}
