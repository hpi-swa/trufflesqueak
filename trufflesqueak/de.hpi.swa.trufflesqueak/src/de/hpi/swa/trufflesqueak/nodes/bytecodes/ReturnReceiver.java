package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class ReturnReceiver extends SqueakBytecodeNode {

    public ReturnReceiver(CompiledMethodObject compiledMethodObject, int idx) {
        super(compiledMethodObject, idx);
    }

    @Override
    public void executeGeneric(VirtualFrame frame) throws LocalReturn {
        throw new LocalReturn(getReceiver(frame));
    }

}
