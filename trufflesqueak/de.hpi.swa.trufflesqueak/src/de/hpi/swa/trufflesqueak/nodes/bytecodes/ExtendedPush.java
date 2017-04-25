package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

public class ExtendedPush extends ExtendedAccess {

    public ExtendedPush(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        Object obj = null;
        switch (type) {
            case 0:
                try {
                    obj = SqueakTypesGen.expectBaseSqueakObject(getReceiver(frame)).at0(storeIdx);
                } catch (UnexpectedResultException e) {
                    throw new RuntimeException("unexpected receiver in object access");
                }
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
