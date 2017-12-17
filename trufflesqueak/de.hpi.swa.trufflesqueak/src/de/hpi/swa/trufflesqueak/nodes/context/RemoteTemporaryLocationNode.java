package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class RemoteTemporaryLocationNode extends AbstractRemoteTemporaryLocationNode {
    @Child private ObjectAtNode readNode;

    public RemoteTemporaryLocationNode(CompiledCodeObject code, int indexInArray, int indexOfArray) {
        super(code, indexInArray, indexOfArray);
        readNode = ObjectAtNode.create(indexInArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return readNode.executeWith(getTempArrayNode.executeRead(frame));
    }
}
