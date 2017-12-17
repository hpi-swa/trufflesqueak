package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.RemoteTemporaryLocationNode;

public class PushRemoteTempNode extends AbstractPushNode {
    @Child private RemoteTemporaryLocationNode remoteTempNode;

    public PushRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes);
        remoteTempNode = new RemoteTemporaryLocationNode(code, indexInArray, indexOfArray);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, remoteTempNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return String.format("pushTemp: %d inVectorAt: %d", remoteTempNode.getIndexInArray(), remoteTempNode.getIndexOfArray());
    }

}
