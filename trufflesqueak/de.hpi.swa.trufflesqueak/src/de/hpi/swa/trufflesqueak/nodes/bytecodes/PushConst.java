package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class PushConst extends SqueakBytecodeNode {
    private final BaseSqueakObject constant;

    public PushConst(CompiledMethodObject compiledMethodObject, int idx, BaseSqueakObject obj) {
        super(compiledMethodObject, idx);
        constant = obj;
    }

    @Override
    public void executeGeneric(VirtualFrame frame) {
        push(frame, constant);
    }
}
