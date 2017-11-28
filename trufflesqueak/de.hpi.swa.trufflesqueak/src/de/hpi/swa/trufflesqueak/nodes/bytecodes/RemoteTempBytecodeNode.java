package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public abstract class RemoteTempBytecodeNode extends SqueakBytecodeNode {
    @Child FrameSlotReadNode execNode;

    public RemoteTempBytecodeNode(CompiledCodeObject code, int idx) {
        super(code, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return execNode.executeRead(frame);
    }

    protected static FrameSlotReadNode getTempArray(CompiledCodeObject code, int indexOfArray) {
        return FrameSlotReadNode.create(code.getStackSlot(indexOfArray));
    }
}
