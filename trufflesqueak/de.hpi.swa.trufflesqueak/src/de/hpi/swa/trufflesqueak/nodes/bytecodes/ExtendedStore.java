package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ExtendedStore extends ExtendedAccess {
    public ExtendedStore(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        Object top = top(frame);
        switch (type) {
            case 0:
                try {
                    ((BaseSqueakObject) getReceiver(frame)).atput0(storeIdx, (BaseSqueakObject) top); // FIXME
                } catch (UnwrappingError e) {
                    throw new RuntimeException("illegal ExtendedStore bytecode: unwrapping error", e);
                }
                break;
            case 1:
                setTemp(frame, storeIdx, top);
                break;
            case 2:
                throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2");
            case 3:
                BaseSqueakObject assoc = getMethod().getLiteral(storeIdx);
                try {
                    assoc.atput0(1, (BaseSqueakObject) top); // FIXME
                } catch (UnwrappingError e) {
                    throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2", e);
                }
                break;
        }
        return top;
    }
}
