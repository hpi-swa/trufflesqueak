package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnConstantNode extends ReturnNode {

    private final Object constant;

    public ReturnConstantNode(CompiledCodeObject code, int idx, Object obj) {
        super(code, idx);
        constant = obj;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new LocalReturn(constant);
    }
}
