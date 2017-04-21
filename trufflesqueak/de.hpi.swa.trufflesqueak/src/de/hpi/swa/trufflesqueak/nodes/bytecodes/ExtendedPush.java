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
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        Object obj = null;
        switch (type) {
            case 0:
                obj = ((BaseSqueakObject) getReceiver(frame)).at0(storeIdx); // FIXME
                break;
            case 1:
                obj = getTemp(frame, storeIdx);
                break;
            case 2:
                obj = getMethod().getLiteral(storeIdx);
                break;
            case 3:
                BaseSqueakObject assoc = getMethod().getLiteral(storeIdx);
                obj = assoc.at0(1);
                break;
        }
        assert obj != null;
        push(frame, obj);
        return obj;
    }
}
