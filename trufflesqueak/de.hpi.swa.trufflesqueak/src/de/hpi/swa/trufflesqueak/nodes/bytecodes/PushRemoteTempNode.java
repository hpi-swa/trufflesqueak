package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushRemoteTempNode extends RemoteTempBytecodeNode {
    @Child ObjectAtNode readTempNode;

    public PushRemoteTempNode(CompiledCodeObject code, int index, int indexInArray, int indexOfArray) {
        super(code, index, indexOfArray);
        readTempNode = ObjectAtNode.create(indexInArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, readTempNode.executeWith(getTempArray(frame)));
    }
}
