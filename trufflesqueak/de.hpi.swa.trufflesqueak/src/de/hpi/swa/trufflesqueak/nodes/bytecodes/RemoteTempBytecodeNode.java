package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public abstract class RemoteTempBytecodeNode extends SqueakBytecodeNode {
    @Child private FrameSlotReadNode getTempArrayNode;
    @CompilationFinal protected final int indexOfArray;
    @CompilationFinal protected final int indexInArray;

    public RemoteTempBytecodeNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes);
        this.indexOfArray = indexOfArray;
        this.indexInArray = indexInArray;
        getTempArrayNode = FrameSlotReadNode.create(code.getTempSlot(indexOfArray));
    }

    protected Object getTempArray(VirtualFrame frame) {
        return getTempArrayNode.executeRead(frame);
    }
}
