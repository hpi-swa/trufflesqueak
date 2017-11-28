package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnTopFromMethodNode extends ReturnTopFromBlockNode {
    public ReturnTopFromMethodNode(CompiledCodeObject code, int idx) {
        super(code, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (getClosure(frame) == code.image.nil) {
            return super.executeGeneric(frame);
        } else {
            throw new NonLocalReturn(pop(frame), ((BlockClosure) getClosure(frame)).getFrameMarker());
        }
    }
}
