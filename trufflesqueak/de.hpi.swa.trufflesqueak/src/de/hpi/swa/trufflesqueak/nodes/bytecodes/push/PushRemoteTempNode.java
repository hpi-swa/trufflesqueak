package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameTemporaryReadNode;

public class PushRemoteTempNode extends AbstractPushNode {
    @Child private ObjectAtNode remoteTempNode;
    @CompilationFinal private final int indexInArray;
    @CompilationFinal private final int indexOfArray;

    public PushRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes);
        this.indexInArray = indexInArray;
        this.indexOfArray = indexOfArray;
        remoteTempNode = ObjectAtNode.create(indexInArray, FrameTemporaryReadNode.create(code, indexOfArray));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, remoteTempNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return String.format("pushTemp: %d inVectorAt: %d", this.indexInArray, this.indexOfArray);
    }

}
