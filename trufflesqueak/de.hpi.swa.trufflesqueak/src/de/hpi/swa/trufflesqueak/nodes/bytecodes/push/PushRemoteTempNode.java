package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.RemoteTempBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushRemoteTempNode extends RemoteTempBytecodeNode {
    @Child ObjectAtNode readTempNode;

    public PushRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
        readTempNode = ObjectAtNode.create(indexInArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, readTempNode.executeWith(getTempArray(frame)));
    }

    @Override
    public String toString() {
        return String.format("pushTemp: %d inVectorAt: %d", indexInArray, indexOfArray);
    }

}
