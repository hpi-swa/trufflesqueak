package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ReturnTopFromMethod extends ReturnTopFromBlock {
    public ReturnTopFromMethod(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn, NonLocalReturn {
        if (getClosure(frame) == getImage().nil) {
            return super.executeGeneric(frame);
        } else {
            throw new NonLocalReturn(top.executeGeneric(frame), getClosure(frame));
        }
    }
}
