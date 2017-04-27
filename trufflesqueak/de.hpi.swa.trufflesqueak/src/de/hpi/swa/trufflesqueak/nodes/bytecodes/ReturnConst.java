package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ReturnConst extends SqueakBytecodeNode {
    private final BaseSqueakObject object;

    public ReturnConst(CompiledMethodObject compiledMethodObject, int idx, BaseSqueakObject obj) {
        super(compiledMethodObject, idx);
        object = obj;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        throw new LocalReturn(object);
    }

}
