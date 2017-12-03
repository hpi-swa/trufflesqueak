package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public abstract class RemoteTempBytecodeNode extends SqueakBytecodeNode {
    @Child FrameSlotReadNode getTempArrayNode;

    public RemoteTempBytecodeNode(CompiledCodeObject code, int index, int indexOfArray) {
        super(code, index);
        getTempArrayNode = FrameSlotReadNode.create(code.getStackSlot(indexOfArray));
    }

    protected Object getTempArray(VirtualFrame frame) {
        return getTempArrayNode.executeRead(frame);
    }
}
