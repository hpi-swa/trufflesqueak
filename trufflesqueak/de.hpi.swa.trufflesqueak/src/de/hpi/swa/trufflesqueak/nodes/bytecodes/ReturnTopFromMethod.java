package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class ReturnTopFromMethod extends SqueakBytecodeNode {

    public ReturnTopFromMethod(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        BaseSqueakObject result = null;
        try {
            int sp = frame.getInt(method.stackPointerSlot);
            if (sp >= 0) {
                result = (BaseSqueakObject) frame.getObject(method.stackSlots[sp]);
            } else {
                result = (BaseSqueakObject) frame.getObject(method.receiverSlot);
            }
        } catch (FrameSlotTypeException e) {
            assert false;
        }
        return result;
    }

}
