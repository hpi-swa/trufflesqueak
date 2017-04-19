package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ExtendedPush extends ExtendedAccess {

    public ExtendedPush(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
    }

    @Override
    public void executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        switch (type) {
            case 0:
                push(frame, getReceiver(frame).at0(storeIdx));
                break;
            case 1:
                push(frame, getTemp(frame, storeIdx));
                break;
            case 2:
                push(frame, getMethod().getLiteral(storeIdx));
                break;
            case 3:
                BaseSqueakObject assoc = getMethod().getLiteral(storeIdx);
                push(frame, assoc.at0(1));
                break;
        }
    }
}
