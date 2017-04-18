package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class PushReceiver extends SqueakBytecodeNode {
    public PushReceiver(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        try {
            int sp = frame.getInt(method.stackPointerSlot);
            frame.setObject(method.stackSlots[sp], frame.getObject(method.receiverSlot));
            frame.setInt(method.stackPointerSlot, sp + 1);
        } catch (FrameSlotTypeException e) {
            assert false;
        }
        return next.executeGeneric(frame);
    }
}
