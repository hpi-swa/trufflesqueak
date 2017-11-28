package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnReceiverNode extends ReturnNode {
    public ReturnReceiverNode(CompiledCodeObject code, int idx) {
        super(code, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new LocalReturn(receiver(frame));
    }
}
