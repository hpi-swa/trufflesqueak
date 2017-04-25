package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ReturnReceiver extends SqueakBytecodeNode {

    public ReturnReceiver(CompiledMethodObject compiledMethodObject, int idx) {
        super(compiledMethodObject, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        throw new LocalReturn(getReceiver(frame));
    }

}
