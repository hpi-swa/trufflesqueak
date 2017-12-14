package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnTopFromBlockNode extends ReturnNode {

    public ReturnTopFromBlockNode(CompiledCodeObject code, int index) {
        super(code, index);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new LocalReturn(pop(frame));
    }

    @Override
    public String toString() {
        return "blockReturn";
    }
}