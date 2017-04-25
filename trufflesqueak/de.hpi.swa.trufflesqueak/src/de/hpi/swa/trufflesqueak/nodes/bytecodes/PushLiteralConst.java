package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PushLiteralConst extends SqueakBytecodeNode {
    private final BaseSqueakObject object;

    public PushLiteralConst(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx);
        object = compiledMethodObject.getLiteral(i & 31);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        push(frame, object);
        return object;
    }
}
