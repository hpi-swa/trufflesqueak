package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class ReturnReceiver extends SqueakBytecodeNode {
    @Child FrameSlotReadNode receiver;

    public ReturnReceiver(CompiledMethodObject compiledMethodObject, int idx) {
        super(compiledMethodObject, idx);
        receiver = FrameSlotReadNode.receiver(compiledMethodObject);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        throw new LocalReturn(receiver.executeGeneric(frame));
    }

}
